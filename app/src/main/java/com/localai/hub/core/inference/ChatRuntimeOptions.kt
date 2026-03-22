package com.localai.hub.core.inference

data class ChatRuntimeOptions(
    val maxTokens: Int = 384,
    val temperature: Float = 0.7f,
    val contextSize: Int = 2048,
    val profile: PerformanceProfile = PerformanceProfile.BALANCED,
)

