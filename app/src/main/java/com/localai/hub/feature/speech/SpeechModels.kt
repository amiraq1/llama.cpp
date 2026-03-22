package com.localai.hub.feature.speech

import com.localai.hub.core.modelregistry.LocalModel

data class SpeechUiState(
    val activeModel: LocalModel? = null,
    val clipLabel: String = "meeting-note-01.m4a",
    val languageHint: String = "ar,en",
    val result: String? = null,
    val isRunning: Boolean = false,
)

