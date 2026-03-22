package com.localai.hub.core.storage

import com.localai.hub.core.modelregistry.LocalModel
import com.localai.hub.core.modelregistry.ModelType
import kotlinx.coroutines.flow.Flow

interface ModelRepository {
    fun observeCatalog(): Flow<List<LocalModel>>
    fun observeActiveModel(type: ModelType): Flow<LocalModel?>
    suspend fun getActiveModel(type: ModelType): LocalModel?
    suspend fun seedCatalogIfNeeded()
    suspend fun queueDownload(modelId: String)
    suspend fun setDownloading(modelId: String)
    suspend fun markDownloaded(modelId: String, localPath: String, version: String)
    suspend fun deleteModel(modelId: String)
    suspend fun setActiveModel(modelId: String)
}

