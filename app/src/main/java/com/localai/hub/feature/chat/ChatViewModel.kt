package com.localai.hub.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arm.aichat.BackendPreference
import com.arm.aichat.FlashAttentionPreference
import com.localai.hub.core.inference.ChatRuntimeOptions
import com.localai.hub.core.inference.InferenceInput
import com.localai.hub.core.inference.InferenceOrchestrator
import com.localai.hub.core.inference.InferenceResult
import com.localai.hub.core.inference.LlamaKvCachePreset
import com.localai.hub.core.inference.PerformanceProfile
import com.localai.hub.core.modelregistry.DownloadStatus
import com.localai.hub.core.modelregistry.ModelType
import com.localai.hub.core.modelregistry.isBundledPreview
import com.localai.hub.core.storage.ModelRepository
import com.localai.hub.core.telemetry.TelemetryMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val modelRepository: ModelRepository,
    private val orchestrator: InferenceOrchestrator,
    telemetryMonitor: TelemetryMonitor,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            modelRepository.seedCatalogIfNeeded()
        }
        viewModelScope.launch {
            modelRepository.observeActiveModel(ModelType.CHAT).collect { model ->
                _uiState.update { state ->
                    state.copy(
                        activeModel = model,
                        notice = if (model?.isBundledPreview == true) {
                            "الوضع الحالي معاينة مدمجة. استورد GGUF من Models لتشغيل llama.cpp الحقيقي."
                        } else {
                            state.notice
                        },
                    )
                }
            }
        }
        viewModelScope.launch {
            telemetryMonitor.stream().collect { telemetry ->
                _uiState.update { state -> state.copy(telemetry = telemetry) }
            }
        }
    }

    fun updateDraft(value: String) {
        _uiState.update { state -> state.copy(draft = value, notice = null) }
    }

    fun updateTemperature(value: Float) {
        _uiState.update { state -> state.copy(temperature = value) }
    }

    fun updateMaxTokens(value: Int) {
        _uiState.update { state -> state.copy(maxTokens = value) }
    }

    fun updateContextSize(value: Int) {
        _uiState.update { state -> state.copy(contextSize = value) }
    }

    fun updateProfile(profile: PerformanceProfile) {
        _uiState.update { state -> state.copy(profile = profile) }
    }

    fun updateBackendPreference(value: BackendPreference) {
        _uiState.update { state -> state.copy(backendPreference = value) }
    }

    fun updateKvCachePreset(value: LlamaKvCachePreset) {
        _uiState.update { state -> state.copy(kvCachePreset = value) }
    }

    fun updateFlashAttention(value: FlashAttentionPreference) {
        _uiState.update { state -> state.copy(flashAttention = value) }
    }

    fun updateUseMmap(value: Boolean) {
        _uiState.update { state -> state.copy(useMmap = value) }
    }

    fun updateUseMlock(value: Boolean) {
        _uiState.update { state -> state.copy(useMlock = value) }
    }

    fun sendMessage() {
        val snapshot = _uiState.value
        val activeModel = snapshot.activeModel
        val prompt = snapshot.draft.trim()

        if (prompt.isBlank()) return
        if (activeModel == null || activeModel.downloadStatus != DownloadStatus.DOWNLOADED) {
            _uiState.update { state ->
                state.copy(
                    notice = "لا يوجد نموذج محادثة محلي جاهز. فعّل أو نزّل نموذجًا من تبويب Models.",
                )
            }
            return
        }

        val userMessage = ChatMessage(
            id = System.currentTimeMillis(),
            role = MessageRole.USER,
            text = prompt,
        )

        _uiState.update { state ->
            state.copy(
                draft = "",
                isGenerating = true,
                notice = null,
                messages = state.messages + userMessage,
            )
        }

        viewModelScope.launch {
            try {
                val result = orchestrator.run(
                    model = activeModel,
                    input = InferenceInput.Chat(
                        prompt = prompt,
                        history = _uiState.value.messages.map(ChatMessage::text),
                        options = ChatRuntimeOptions(
                            maxTokens = snapshot.maxTokens,
                            temperature = snapshot.temperature,
                            contextSize = snapshot.contextSize,
                            profile = snapshot.profile,
                            backendPreference = snapshot.backendPreference,
                            kvCachePreset = snapshot.kvCachePreset,
                            flashAttention = snapshot.flashAttention,
                            useMmap = snapshot.useMmap,
                            useMlock = snapshot.useMlock,
                        ),
                    ),
                ) as InferenceResult.Text

                val tokensPerSecond = if (result.latencyMs > 0 && result.generatedUnits > 0) {
                    result.generatedUnits * 1000.0 / result.latencyMs.toDouble()
                } else {
                    null
                }

                _uiState.update { state ->
                    state.copy(
                        isGenerating = false,
                        lastLatencyMs = result.latencyMs,
                        lastTokensPerSecond = tokensPerSecond,
                        lastEngineLabel = result.engineLabel,
                        messages = state.messages + ChatMessage(
                            id = System.currentTimeMillis() + 1,
                            role = MessageRole.ASSISTANT,
                            text = result.content,
                        ),
                    )
                }
            } catch (error: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isGenerating = false,
                        notice = error.message ?: "تعذر إكمال الرد المحلي.",
                    )
                }
            }
        }
    }
}
