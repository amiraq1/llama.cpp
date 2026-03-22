package com.localai.hub.feature.chat

import com.arm.aichat.BackendPreference
import com.arm.aichat.FlashAttentionPreference
import com.localai.hub.core.inference.PerformanceProfile
import com.localai.hub.core.inference.LlamaKvCachePreset
import com.localai.hub.core.modelregistry.LocalModel
import com.localai.hub.core.telemetry.DeviceTelemetry

enum class MessageRole {
    USER,
    ASSISTANT,
}

data class ChatMessage(
    val id: Long,
    val role: MessageRole,
    val text: String,
)

data class ChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            id = 0L,
            role = MessageRole.ASSISTANT,
            text = "هذا MVP لتطبيق ذكاء اصطناعي محلي. الدردشة تعمل الآن بمعاينة محلية افتراضيًا، " +
                "ويمكن تحويلها إلى llama.cpp فعلي بعد استيراد ملف GGUF من تبويب Models.",
        ),
    ),
    val draft: String = "",
    val activeModel: LocalModel? = null,
    val telemetry: DeviceTelemetry? = null,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 384,
    val contextSize: Int = 2048,
    val profile: PerformanceProfile = PerformanceProfile.BALANCED,
    val backendPreference: BackendPreference = BackendPreference.AUTO,
    val kvCachePreset: LlamaKvCachePreset = LlamaKvCachePreset.AUTO,
    val flashAttention: FlashAttentionPreference = FlashAttentionPreference.AUTO,
    val useMmap: Boolean = true,
    val useMlock: Boolean = false,
    val isGenerating: Boolean = false,
    val lastLatencyMs: Long? = null,
    val lastTokensPerSecond: Double? = null,
    val lastEngineLabel: String? = null,
    val notice: String? = null,
)
