package com.localai.hub.core.modelregistry

data class ModelSeed(
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
    val downloadedByDefault: Boolean = false,
    val activeByDefault: Boolean = false,
    val version: String? = null,
)

