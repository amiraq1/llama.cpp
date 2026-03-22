package com.localai.hub.core.modelregistry

data class LocalModel(
    val id: String,
    val name: String,
    val type: ModelType,
    val format: ModelFormat,
    val engine: RuntimeEngine,
    val sizeMb: Int,
    val languageSupport: List<String>,
    val minRamGb: Int,
    val supportsGpu: Boolean,
    val supportsNpu: Boolean,
    val description: String,
    val downloadStatus: DownloadStatus,
    val version: String?,
    val localPath: String?,
    val isActive: Boolean,
)

val LocalModel.isBundledPreview: Boolean
    get() = localPath?.startsWith("bundled://") == true

val LocalModel.hasRealLocalFile: Boolean
    get() = !localPath.isNullOrBlank() && !isBundledPreview

val LocalModel.displayName: String
    get() = if (
        engine == RuntimeEngine.LLAMA_CPP &&
        hasRealLocalFile &&
        !version.isNullOrBlank() &&
        version != "bundled-preview"
    ) {
        version.removeSuffix(".gguf")
    } else {
        name
    }
