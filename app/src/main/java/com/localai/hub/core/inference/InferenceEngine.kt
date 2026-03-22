package com.localai.hub.core.inference

import com.localai.hub.core.modelregistry.LocalModel
import com.localai.hub.core.modelregistry.RuntimeEngine

interface InferenceEngine {
    val backend: RuntimeEngine
    suspend fun run(model: LocalModel, input: InferenceInput): InferenceResult
}

