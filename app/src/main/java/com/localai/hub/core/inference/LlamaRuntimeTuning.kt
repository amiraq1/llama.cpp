package com.localai.hub.core.inference

enum class LlamaKvCachePreset(
    val displayName: String,
) {
    AUTO("KV تلقائي"),
    Q4("KV 4-bit"),
    Q8("KV 8-bit"),
    F16("KV F16"),
}
