package com.localai.hub.feature.speech

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localai.hub.core.inference.InferenceInput
import com.localai.hub.core.inference.InferenceOrchestrator
import com.localai.hub.core.inference.InferenceResult
import com.localai.hub.core.modelregistry.DownloadStatus
import com.localai.hub.core.modelregistry.ModelType
import com.localai.hub.core.storage.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SpeechViewModel @Inject constructor(
    private val repository: ModelRepository,
    private val orchestrator: InferenceOrchestrator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SpeechUiState())
    val uiState: StateFlow<SpeechUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedCatalogIfNeeded()
        }
        viewModelScope.launch {
            repository.observeActiveModel(ModelType.SPEECH_TO_TEXT).collect { model ->
                _uiState.update { state -> state.copy(activeModel = model) }
            }
        }
    }

    fun updateClipLabel(value: String) {
        _uiState.update { state -> state.copy(clipLabel = value) }
    }

    fun updateLanguageHint(value: String) {
        _uiState.update { state -> state.copy(languageHint = value) }
    }

    fun runDemo() {
        val snapshot = _uiState.value
        val model = snapshot.activeModel
        if (model == null || model.downloadStatus != DownloadStatus.DOWNLOADED) {
            _uiState.update { state ->
                state.copy(result = "نزّل أو فعّل نموذج Speech-to-Text من تبويب Models أولًا.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { state -> state.copy(isRunning = true) }
            val result = orchestrator.run(
                model = model,
                input = InferenceInput.AudioTranscription(
                    clipLabel = snapshot.clipLabel,
                    languageHint = snapshot.languageHint,
                ),
            ) as InferenceResult.Text
            _uiState.update { state ->
                state.copy(
                    isRunning = false,
                    result = result.content,
                )
            }
        }
    }
}

