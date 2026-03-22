package com.localai.hub.core.inference

import com.localai.hub.core.modelregistry.LocalModel
import com.localai.hub.core.modelregistry.RuntimeEngine
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

@Singleton
class OnnxRuntimeEngine @Inject constructor() : InferenceEngine {
    override val backend: RuntimeEngine = RuntimeEngine.ONNX_RUNTIME

    override suspend fun run(model: LocalModel, input: InferenceInput): InferenceResult {
        delay(480L)

        val content = when (input) {
            is InferenceInput.AudioTranscription -> {
                "نسخ محلي تجريبي من ${input.clipLabel}: " +
                    "\"مرحبا، هذا مثال على تفريغ الصوت محليًا مع تلميح اللغة ${input.languageHint}\"."
            }

            is InferenceInput.Chat -> {
                "مسار ONNX جاهز للترجمة أو نماذج النص الخفيفة عبر ${model.name}."
            }

            else -> {
                "محرك ONNX Runtime جاهز للتوسعة لنماذج الرؤية أو الترجمة."
            }
        }

        return InferenceResult.Text(
            content = content,
            latencyMs = 480L,
            engineLabel = "ONNX Runtime",
        )
    }
}

