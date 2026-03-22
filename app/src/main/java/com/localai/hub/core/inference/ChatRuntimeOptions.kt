package com.localai.hub.core.inference

import com.arm.aichat.BackendPreference
import com.arm.aichat.FlashAttentionPreference

data class ChatRuntimeOptions(
    val maxTokens: Int = 384,
    val temperature: Float = 0.7f,
    val contextSize: Int = 2048,
    val profile: PerformanceProfile = PerformanceProfile.BALANCED,
    val backendPreference: BackendPreference = BackendPreference.AUTO,
    val kvCachePreset: LlamaKvCachePreset = LlamaKvCachePreset.AUTO,
    val flashAttention: FlashAttentionPreference = FlashAttentionPreference.AUTO,
    val useMmap: Boolean = true,
    val useMlock: Boolean = false,
)
