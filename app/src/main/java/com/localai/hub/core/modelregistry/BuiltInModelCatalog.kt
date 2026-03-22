package com.localai.hub.core.modelregistry

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuiltInModelCatalog @Inject constructor() {
    private val builtIns by lazy {
        listOf(
            ModelSeed(
                id = "tinyllama-chat-q4",
                name = "TinyLlama Chat Q4",
                type = ModelType.CHAT,
                format = ModelFormat.GGUF,
                engine = RuntimeEngine.LLAMA_CPP,
                sizeMb = 1200,
                languageSupport = listOf("ar", "en"),
                minRamGb = 4,
                supportsGpu = false,
                supportsNpu = false,
                description = "نموذج محادثة خفيف للمساعد المحلي والملخصات القصيرة.",
                downloadedByDefault = true,
                activeByDefault = true,
                version = "bundled-preview",
            ),
            ModelSeed(
                id = "ocr-lite-ar-en",
                name = "OCR Lite Arabic/English",
                type = ModelType.OCR,
                format = ModelFormat.TFLITE,
                engine = RuntimeEngine.LITERT,
                sizeMb = 48,
                languageSupport = listOf("ar", "en"),
                minRamGb = 3,
                supportsGpu = true,
                supportsNpu = true,
                description = "OCR سريع للفواتير والملاحظات والصور البسيطة.",
            ),
            ModelSeed(
                id = "whisper-mini-onnx",
                name = "Whisper Mini ONNX",
                type = ModelType.SPEECH_TO_TEXT,
                format = ModelFormat.ONNX,
                engine = RuntimeEngine.ONNX_RUNTIME,
                sizeMb = 86,
                languageSupport = listOf("ar", "en"),
                minRamGb = 4,
                supportsGpu = true,
                supportsNpu = false,
                description = "نسخ صوت محلي للملاحظات القصيرة والمقاطع الصوتية.",
            ),
            ModelSeed(
                id = "translate-ar-en-mini",
                name = "Translate AR/EN Mini",
                type = ModelType.TRANSLATION,
                format = ModelFormat.ONNX,
                engine = RuntimeEngine.ONNX_RUNTIME,
                sizeMb = 112,
                languageSupport = listOf("ar", "en"),
                minRamGb = 3,
                supportsGpu = true,
                supportsNpu = false,
                description = "ترجمة محلية ثنائية الاتجاه للاستخدام السريع دون إنترنت.",
            ),
        )
    }

    fun seeds(): List<ModelSeed> {
        return builtIns
    }

    fun seedFor(modelId: String): ModelSeed? = builtIns.firstOrNull { it.id == modelId }
}
