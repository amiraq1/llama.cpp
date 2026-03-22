package com.localai.hub.core.inference

sealed interface InferenceInput {
    data class Chat(
        val prompt: String,
        val history: List<String>,
        val options: ChatRuntimeOptions,
    ) : InferenceInput

    data class ImageScan(
        val assetLabel: String,
        val instruction: String,
    ) : InferenceInput

    data class AudioTranscription(
        val clipLabel: String,
        val languageHint: String,
    ) : InferenceInput
}

