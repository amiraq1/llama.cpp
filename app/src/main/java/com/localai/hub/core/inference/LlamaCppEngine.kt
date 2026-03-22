package com.localai.hub.core.inference

import android.app.ActivityManager
import android.content.Context
import com.arm.aichat.AiChat
import com.arm.aichat.BackendPreference
import com.arm.aichat.FlashAttentionPreference
import com.arm.aichat.InferenceEngine.State
import com.arm.aichat.KvCacheType
import com.arm.aichat.ModelLoadOptions
import com.arm.aichat.gguf.GgufMetadataReader
import com.arm.aichat.isModelLoaded
import com.localai.hub.core.modelregistry.LocalModel
import com.localai.hub.core.modelregistry.RuntimeEngine
import com.localai.hub.core.modelregistry.displayName
import com.localai.hub.core.modelregistry.hasRealLocalFile
import com.localai.hub.core.modelregistry.isBundledPreview
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.ceil
import kotlin.system.measureTimeMillis

@Singleton
class LlamaCppEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : InferenceEngine {
    override val backend: RuntimeEngine = RuntimeEngine.LLAMA_CPP

    private val runtime by lazy { AiChat.getInferenceEngine(context) }
    private val runtimeMutex = Mutex()
    private val metadataReader = GgufMetadataReader.create(
        skipKeys = setOf(
            "tokenizer.ggml.scores",
            "tokenizer.ggml.tokens",
            "tokenizer.ggml.token_type",
        ),
    )

    private val metadataCache = mutableMapOf<String, CachedGgufMetadata>()

    private var loadedModelPath: String? = null
    private var loadedRuntimeSignature: LoadedRuntimeSignature? = null

    override suspend fun run(model: LocalModel, input: InferenceInput): InferenceResult {
        val chatInput = input as? InferenceInput.Chat
            ?: return InferenceResult.Text(
                content = "محرك llama.cpp في هذا التطبيق مخصص للمحادثة المحلية.",
                latencyMs = 0L,
                engineLabel = backend.name,
            )

        if (model.isBundledPreview || !model.hasRealLocalFile) {
            return previewResponse(model = model, chatInput = chatInput)
        }

        return runtimeMutex.withLock {
            runRealModel(model = model, chatInput = chatInput)
        }
    }

    private suspend fun runRealModel(
        model: LocalModel,
        chatInput: InferenceInput.Chat,
    ): InferenceResult.Text {
        val localPath = model.localPath
            ?: return previewResponse(model = model, chatInput = chatInput)

        val modelFile = File(localPath)
        if (!modelFile.exists() || !modelFile.canRead()) {
            clearRuntimeCache()
            return InferenceResult.Text(
                content = "تعذر قراءة ملف GGUF المحلي. أعد استيراد النموذج من تبويب Models.",
                latencyMs = 0L,
                engineLabel = "llama.cpp",
            )
        }

        return try {
            awaitRuntimeReady()

            val metadata = readCachedMetadata(modelFile)
            val runtimePlan = resolveRuntimePlan(modelFile, metadata, chatInput.options)
            val needsReload = loadedModelPath != localPath || loadedRuntimeSignature != runtimePlan.signature

            if (needsReload) {
                resetRuntimeIfNeeded()
                runtime.loadModel(
                    pathToModel = localPath,
                    options = runtimePlan.loadOptions,
                )
                runtime.setSystemPrompt(DEFAULT_SYSTEM_PROMPT)
                loadedModelPath = localPath
                loadedRuntimeSignature = runtimePlan.signature
            }

            val response = StringBuilder()
            var generatedUnits = 0
            val latencyMs = measureTimeMillis {
                runtime.sendUserPrompt(
                    message = chatInput.prompt.trim(),
                    predictLength = chatInput.options.maxTokens,
                ).collect { token ->
                    response.append(token)
                    generatedUnits++
                }
            }

            val activeBackend = runtime.activeBackendName()
            InferenceResult.Text(
                content = response.toString().trim().ifBlank {
                    "لم يرجع النموذج نصًا. جرّب `max tokens` أعلى أو خفّف طول الرسالة."
                },
                latencyMs = latencyMs,
                engineLabel = buildEngineLabel(activeBackend, runtimePlan, metadata),
                generatedUnits = generatedUnits,
            )
        } catch (error: Exception) {
            clearRuntimeCache()
            InferenceResult.Text(
                content = buildString {
                    append("فشل تشغيل llama.cpp لهذا النموذج.\n\n")
                    append(error.message ?: "خطأ غير معروف")
                    append("\n\n")
                    append("جرّب إعادة استيراد ملف GGUF أو تقليل context size.")
                },
                latencyMs = 0L,
                engineLabel = "llama.cpp",
            )
        }
    }

    private suspend fun readCachedMetadata(modelFile: File): CachedGgufMetadata? {
        metadataCache[modelFile.absolutePath]?.let { return it }

        val metadataInput = modelFile.inputStream().buffered()
        val metadata = try {
            metadataReader.readStructuredMetadata(metadataInput)
        } catch (_: Exception) {
            null
        } finally {
            metadataInput.close()
        }

        return metadata?.let {
            CachedGgufMetadata(
                displayName = it.basic.nameLabel ?: it.basic.name,
                contextLength = it.dimensions?.contextLength,
                hasChatTemplate = !it.tokenizer?.chatTemplate.isNullOrBlank(),
                fileType = it.architecture?.fileType,
            ).also {
                metadataCache[modelFile.absolutePath] = it
            }
        }
    }

    private fun resolveRuntimePlan(
        modelFile: File,
        metadata: CachedGgufMetadata?,
        options: ChatRuntimeOptions,
    ): ResolvedRuntimePlan {
        val memoryInfo = ActivityManager.MemoryInfo().also(activityManager()::getMemoryInfo)
        val totalRamMb = memoryInfo.totalMem.toMb()
        val freeRamMb = memoryInfo.availMem.toMb()
        val modelSizeMb = ceil(modelFile.length().toDouble() / BYTES_PER_MB).toInt()

        val modelContextCap = metadata?.contextLength?.coerceAtLeast(512) ?: options.contextSize
        val safeContextCap = when {
            totalRamMb <= 6_144 -> 2_048
            totalRamMb <= 8_192 -> 4_096
            else -> 8_192
        }
        val runtimeContextCap = if (modelSizeMb + 1_536 > freeRamMb) {
            minOf(safeContextCap, 2_048)
        } else {
            safeContextCap
        }
        val resolvedContext = minOf(
            options.contextSize.coerceAtLeast(512),
            modelContextCap,
            runtimeContextCap,
        ).coerceAtLeast(512)

        val resolvedCacheTypes = resolveCacheTypes(
            preset = options.kvCachePreset,
            profile = options.profile,
            freeRamMb = freeRamMb,
            modelSizeMb = modelSizeMb,
        )
        val resolvedFlashAttention = when {
            resolvedCacheTypes.second != KvCacheType.F16 &&
                options.flashAttention == FlashAttentionPreference.DISABLED -> {
                FlashAttentionPreference.AUTO
            }

            else -> options.flashAttention
        }

        val cpuCount = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
        val resolvedThreads = when (options.profile) {
            PerformanceProfile.FAST -> minOf((cpuCount - 1).coerceAtLeast(2), 8)
            PerformanceProfile.BALANCED -> minOf((cpuCount - 2).coerceAtLeast(2), 6)
            PerformanceProfile.ACCURATE -> minOf((cpuCount - 1).coerceAtLeast(2), 8)
        }
        val resolvedBatchSize = when (options.profile) {
            PerformanceProfile.FAST -> 256
            PerformanceProfile.BALANCED -> 512
            PerformanceProfile.ACCURATE -> 768
        }

        val resolvedUseMlock = options.useMlock && freeRamMb > modelSizeMb + 2_048
        val resolvedGpuLayers = when (options.backendPreference) {
            BackendPreference.CPU -> 0
            else -> -1
        }

        val loadOptions = ModelLoadOptions(
            contextSize = resolvedContext,
            temperature = options.temperature,
            backendPreference = options.backendPreference,
            gpuLayers = resolvedGpuLayers,
            threads = resolvedThreads,
            batchSize = resolvedBatchSize,
            cacheTypeK = resolvedCacheTypes.first,
            cacheTypeV = resolvedCacheTypes.second,
            flashAttention = resolvedFlashAttention,
            useMmap = options.useMmap,
            useMlock = resolvedUseMlock,
        )

        return ResolvedRuntimePlan(
            loadOptions = loadOptions,
            signature = LoadedRuntimeSignature(
                modelPath = modelFile.absolutePath,
                contextSize = loadOptions.contextSize,
                temperature = loadOptions.temperature,
                backendPreference = loadOptions.backendPreference,
                gpuLayers = loadOptions.gpuLayers,
                threads = loadOptions.threads,
                batchSize = loadOptions.batchSize,
                cacheTypeK = loadOptions.cacheTypeK,
                cacheTypeV = loadOptions.cacheTypeV,
                flashAttention = loadOptions.flashAttention,
                useMmap = loadOptions.useMmap,
                useMlock = loadOptions.useMlock,
            ),
        )
    }

    private fun resolveCacheTypes(
        preset: LlamaKvCachePreset,
        profile: PerformanceProfile,
        freeRamMb: Int,
        modelSizeMb: Int,
    ): Pair<KvCacheType, KvCacheType> {
        val tightBudget = freeRamMb < modelSizeMb + 1_024
        val mediumBudget = freeRamMb < modelSizeMb + 2_048

        return when (preset) {
            LlamaKvCachePreset.Q4 -> KvCacheType.Q4_0 to KvCacheType.Q4_0
            LlamaKvCachePreset.Q8 -> KvCacheType.Q8_0 to KvCacheType.Q8_0
            LlamaKvCachePreset.F16 -> KvCacheType.F16 to KvCacheType.F16
            LlamaKvCachePreset.AUTO -> when {
                tightBudget -> KvCacheType.Q4_0 to KvCacheType.Q4_0
                profile == PerformanceProfile.FAST -> KvCacheType.Q4_0 to KvCacheType.Q4_0
                mediumBudget -> KvCacheType.Q8_0 to KvCacheType.Q8_0
                profile == PerformanceProfile.ACCURATE -> KvCacheType.F16 to KvCacheType.F16
                else -> KvCacheType.Q8_0 to KvCacheType.Q8_0
            }
        }
    }

    private fun buildEngineLabel(
        activeBackend: String,
        runtimePlan: ResolvedRuntimePlan,
        metadata: CachedGgufMetadata?,
    ): String {
        val opts = runtimePlan.loadOptions
        val templateLabel = if (metadata?.hasChatTemplate == true) "tmpl:embedded" else "tmpl:default"
        val fileTypeLabel = metadata?.fileType?.let { "ftype:$it" } ?: "ftype:?"
        return buildString {
            append("llama.cpp")
            append(" | ")
            append(activeBackend.ifBlank { "CPU" })
            append(" | ctx ")
            append(opts.contextSize)
            append(" | KV ")
            append(opts.cacheTypeK.name)
            append("/")
            append(opts.cacheTypeV.name)
            append(" | mmap ")
            append(if (opts.useMmap) "on" else "off")
            append(" | ")
            append(templateLabel)
            append(" | ")
            append(fileTypeLabel)
            metadata?.displayName?.let {
                append(" | ")
                append(it)
            }
        }
    }

    private fun activityManager(): ActivityManager {
        return context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }

    private suspend fun awaitRuntimeReady() {
        val currentState = runtime.state.value
        if (currentState is State.Initialized || currentState.isModelLoaded) {
            return
        }
        if (currentState is State.Error) {
            throw currentState.exception
        }

        val readyState = runtime.state
            .filter { state ->
                state is State.Initialized || state.isModelLoaded || state is State.Error
            }
            .first()

        if (readyState is State.Error) {
            throw readyState.exception
        }
    }

    private fun resetRuntimeIfNeeded() {
        when (runtime.state.value) {
            is State.ModelReady,
            is State.Benchmarking,
            is State.ProcessingSystemPrompt,
            is State.ProcessingUserPrompt,
            is State.Generating,
            is State.Error,
            -> runtime.cleanUp()

            else -> Unit
        }
        clearRuntimeCache()
    }

    private fun clearRuntimeCache() {
        loadedModelPath = null
        loadedRuntimeSignature = null
    }

    private fun previewResponse(
        model: LocalModel,
        chatInput: InferenceInput.Chat,
    ): InferenceResult.Text {
        val latency = chatInput.options.profile.simulatedDelayMs
        val prompt = chatInput.prompt.trim()
        val summary = when {
            prompt.length > 180 -> "ملخص محلي: ${prompt.take(150)}..."
            prompt.contains("لخّص", ignoreCase = true) || prompt.contains("summar", ignoreCase = true) ->
                "الملخص السريع: ${prompt.take(100)}"

            else -> "استجابة محلية تجريبية: ${prompt.take(110)}"
        }
        val tunedTemperature = String.format(Locale.US, "%.1f", chatInput.options.temperature)

        return InferenceResult.Text(
            content = buildString {
                append(summary)
                append("\n\n")
                append("النموذج: ${model.displayName}")
                append(" | الوضع: ${chatInput.options.profile.displayName}")
                append(" | الحرارة: $tunedTemperature")
                append(" | السياق: ${chatInput.options.contextSize}")
                append("\n")
                append("ملاحظة: هذه معاينة مدمجة. استورد GGUF من تبويب Models لتفعيل llama.cpp الحقيقي.")
            },
            latencyMs = latency,
            engineLabel = "llama.cpp-preview",
        )
    }

    private data class CachedGgufMetadata(
        val displayName: String?,
        val contextLength: Int?,
        val hasChatTemplate: Boolean,
        val fileType: Int?,
    )

    private data class ResolvedRuntimePlan(
        val loadOptions: ModelLoadOptions,
        val signature: LoadedRuntimeSignature,
    )

    private data class LoadedRuntimeSignature(
        val modelPath: String,
        val contextSize: Int,
        val temperature: Float,
        val backendPreference: BackendPreference,
        val gpuLayers: Int,
        val threads: Int,
        val batchSize: Int,
        val cacheTypeK: KvCacheType,
        val cacheTypeV: KvCacheType,
        val flashAttention: FlashAttentionPreference,
        val useMmap: Boolean,
        val useMlock: Boolean,
    )

    private companion object {
        const val DEFAULT_SYSTEM_PROMPT =
            "You are Local AI Hub, an offline assistant. Reply in the user's language, stay concise, " +
                "and prefer factual answers."
        const val BYTES_PER_MB = 1024.0 * 1024.0
    }
}

private fun Long.toMb(): Int = (this / 1024L / 1024L).toInt()
