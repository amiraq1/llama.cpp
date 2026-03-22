package com.localai.hub.core.inference

import android.content.Context
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine.State
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
import kotlin.math.abs
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.system.measureTimeMillis

@Singleton
class LlamaCppEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : InferenceEngine {
    override val backend: RuntimeEngine = RuntimeEngine.LLAMA_CPP

    private val runtime by lazy { AiChat.getInferenceEngine(context) }
    private val runtimeMutex = Mutex()

    private var loadedModelPath: String? = null
    private var loadedContextSize: Int? = null
    private var loadedTemperature: Float? = null

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

            val requestedTemperature = chatInput.options.temperature.coerceIn(0.05f, 2.0f)
            val needsReload = loadedModelPath != localPath ||
                loadedContextSize != chatInput.options.contextSize ||
                loadedTemperature == null ||
                abs(loadedTemperature!! - requestedTemperature) > 0.01f

            if (needsReload) {
                resetRuntimeIfNeeded()
                runtime.loadModel(
                    pathToModel = localPath,
                    contextSize = chatInput.options.contextSize,
                    temperature = requestedTemperature,
                )
                runtime.setSystemPrompt(DEFAULT_SYSTEM_PROMPT)
                loadedModelPath = localPath
                loadedContextSize = chatInput.options.contextSize
                loadedTemperature = requestedTemperature
            }

            val response = StringBuilder()
            val latencyMs = measureTimeMillis {
                runtime.sendUserPrompt(
                    message = chatInput.prompt.trim(),
                    predictLength = chatInput.options.maxTokens,
                ).collect { token ->
                    response.append(token)
                }
            }

            InferenceResult.Text(
                content = response.toString().trim().ifBlank {
                    "لم يرجع النموذج نصًا. جرّب `max tokens` أعلى أو خفّف طول الرسالة."
                },
                latencyMs = latencyMs,
                engineLabel = "llama.cpp",
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
        loadedContextSize = null
        loadedTemperature = null
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

    private companion object {
        const val DEFAULT_SYSTEM_PROMPT =
            "You are Local AI Hub, an offline assistant. Reply in the user's language, stay concise, " +
                "and prefer factual answers."
    }
}
