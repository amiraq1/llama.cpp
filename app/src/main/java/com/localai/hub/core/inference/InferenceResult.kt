package com.localai.hub.core.inference

sealed interface InferenceResult {
    data class Text(
        val content: String,
        val latencyMs: Long,
        val engineLabel: String,
        val generatedUnits: Int = 0,
    ) : InferenceResult
}
