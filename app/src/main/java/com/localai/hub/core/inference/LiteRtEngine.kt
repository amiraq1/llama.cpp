package com.localai.hub.core.inference

import com.localai.hub.core.modelregistry.LocalModel
import com.localai.hub.core.modelregistry.RuntimeEngine
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

@Singleton
class LiteRtEngine @Inject constructor() : InferenceEngine {
    override val backend: RuntimeEngine = RuntimeEngine.LITERT

    override suspend fun run(model: LocalModel, input: InferenceInput): InferenceResult {
        delay(420L)

        val content = when (input) {
            is InferenceInput.ImageScan -> {
                "OCR تجريبي من ${input.assetLabel}: تم استخراج 3 أسطر، منها عنوان ووقت وإجمالي. " +
                    "التوجيه المستخدم: ${input.instruction.ifBlank { "قراءة النصوص الأساسية فقط" }}."
            }

            else -> {
                "LiteRT جاهز لتشغيل مسارات الرؤية والـ OCR على الجهاز عبر ${model.name}."
            }
        }

        return InferenceResult.Text(
            content = content,
            latencyMs = 420L,
            engineLabel = "LiteRT",
        )
    }
}

