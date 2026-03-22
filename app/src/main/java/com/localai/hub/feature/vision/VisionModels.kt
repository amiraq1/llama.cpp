package com.localai.hub.feature.vision

import com.localai.hub.core.modelregistry.LocalModel

data class VisionUiState(
    val activeModel: LocalModel? = null,
    val sampleLabel: String = "receipt_0322.jpg",
    val instruction: String = "استخرج النص والعناصر المهمة فقط",
    val result: String? = null,
    val isRunning: Boolean = false,
)

