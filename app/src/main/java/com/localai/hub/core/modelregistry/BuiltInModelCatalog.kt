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
                sizeMb = 3,
                languageSupport = listOf("ar", "en"),
                minRamGb = 3,
                supportsGpu = true,
                supportsNpu = true,
                description = "حزمة تنزيل تجريبية للرؤية المحلية مع تحقق sha256 واستئناف التحميل.",
                downloadUrl = "https://storage.googleapis.com/download.tensorflow.org/models/tflite/coco_ssd_mobilenet_v1_1.0_quant_2018_06_29.zip",
                downloadSha256 = "A809CD290B4D6A2E8A9D5DAD076E0BD695B8091974E0EED1052B480B2F21B6DC",
                downloadFileName = "coco_ssd_mobilenet_v1_1.0_quant_2018_06_29.zip",
            ),
            ModelSeed(
                id = "whisper-mini-onnx",
                name = "Whisper Mini ONNX",
                type = ModelType.SPEECH_TO_TEXT,
                format = ModelFormat.ONNX,
                engine = RuntimeEngine.ONNX_RUNTIME,
                sizeMb = 1,
                languageSupport = listOf("ar", "en"),
                minRamGb = 4,
                supportsGpu = true,
                supportsNpu = false,
                description = "أصل ONNX تجريبي لمسار الصوت مع تنزيل حقيقي قابل للاستئناف.",
                downloadUrl = "https://github.com/onnx/models/raw/main/validated/vision/classification/mnist/model/mnist-8.onnx",
                downloadSha256 = "2F06E72DE813A8635C9BC0397AC447A601BDBFA7DF4BEBC278723B958831C9BF",
                downloadFileName = "mnist-8.onnx",
            ),
            ModelSeed(
                id = "translate-ar-en-mini",
                name = "Translate AR/EN Mini",
                type = ModelType.TRANSLATION,
                format = ModelFormat.ONNX,
                engine = RuntimeEngine.ONNX_RUNTIME,
                sizeMb = 1,
                languageSupport = listOf("ar", "en"),
                minRamGb = 3,
                supportsGpu = true,
                supportsNpu = false,
                description = "أصل ONNX تجريبي لمسار الترجمة مع تنزيل حقيقي وتحقق checksum.",
                downloadUrl = "https://github.com/onnx/models/raw/main/validated/vision/classification/mnist/model/mnist-8.onnx",
                downloadSha256 = "2F06E72DE813A8635C9BC0397AC447A601BDBFA7DF4BEBC278723B958831C9BF",
                downloadFileName = "mnist-8.onnx",
            ),
        )
    }

    fun seeds(): List<ModelSeed> {
        return builtIns
    }

    fun seedFor(modelId: String): ModelSeed? = builtIns.firstOrNull { it.id == modelId }
}
