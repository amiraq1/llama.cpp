package com.localai.hub.core.inference

import com.localai.hub.core.modelregistry.LocalModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InferenceOrchestrator @Inject constructor(
    engines: List<@JvmSuppressWildcards InferenceEngine>,
) {
    private val enginesByBackend = engines.associateBy(InferenceEngine::backend)

    suspend fun run(model: LocalModel, input: InferenceInput): InferenceResult {
        val engine = requireNotNull(enginesByBackend[model.engine]) {
            "No inference engine registered for ${model.engine}"
        }
        return engine.run(model, input)
    }
}

