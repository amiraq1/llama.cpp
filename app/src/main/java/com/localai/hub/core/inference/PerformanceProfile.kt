package com.localai.hub.core.inference

enum class PerformanceProfile(
    val displayName: String,
    val simulatedDelayMs: Long,
) {
    FAST("سريع", 550L),
    BALANCED("متوازن", 900L),
    ACCURATE("دقيق", 1350L),
}

