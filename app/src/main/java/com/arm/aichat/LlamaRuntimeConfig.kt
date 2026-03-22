package com.arm.aichat

enum class BackendPreference {
    AUTO,
    CPU,
    VULKAN,
    OPENCL,
}

enum class KvCacheType {
    F16,
    Q8_0,
    Q4_0,
}

enum class FlashAttentionPreference(
    val nativeValue: Int,
) {
    AUTO(-1),
    DISABLED(0),
    ENABLED(1),
}

data class ModelLoadOptions(
    val contextSize: Int = InferenceEngine.DEFAULT_CONTEXT_SIZE,
    val temperature: Float = InferenceEngine.DEFAULT_TEMPERATURE,
    val backendPreference: BackendPreference = BackendPreference.AUTO,
    val gpuLayers: Int = -1,
    val threads: Int = 0,
    val batchSize: Int = 512,
    val cacheTypeK: KvCacheType = KvCacheType.F16,
    val cacheTypeV: KvCacheType = KvCacheType.F16,
    val flashAttention: FlashAttentionPreference = FlashAttentionPreference.AUTO,
    val useMmap: Boolean = true,
    val useMlock: Boolean = false,
)
