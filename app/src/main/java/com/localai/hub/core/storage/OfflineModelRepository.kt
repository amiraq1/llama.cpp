package com.localai.hub.core.storage

import android.content.Context
import com.localai.hub.core.modelregistry.BuiltInModelCatalog
import com.localai.hub.core.modelregistry.DownloadStatus
import com.localai.hub.core.modelregistry.LocalModel
import com.localai.hub.core.modelregistry.ModelType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class OfflineModelRepository @Inject constructor(
    private val modelDao: ModelDao,
    private val catalog: BuiltInModelCatalog,
    @ApplicationContext private val context: Context,
) : ModelRepository {

    override fun observeCatalog(): Flow<List<LocalModel>> {
        return modelDao.observeAll().map { models -> models.map(ModelEntity::toDomain) }
    }

    override fun observeActiveModel(type: ModelType): Flow<LocalModel?> {
        return modelDao.observeActiveByType(type.name).map { it?.toDomain() }
    }

    override suspend fun getActiveModel(type: ModelType): LocalModel? {
        return modelDao.getActiveByType(type.name)?.toDomain()
    }

    override suspend fun seedCatalogIfNeeded() {
        if (modelDao.count() == 0) {
            modelDao.insertAll(catalog.seeds().map { it.toEntity() })
        }
    }

    override suspend fun queueDownload(modelId: String) {
        modelDao.updateDownloadStatus(modelId, DownloadStatus.QUEUED.name)
    }

    override suspend fun setDownloading(modelId: String) {
        modelDao.updateDownloadStatus(modelId, DownloadStatus.DOWNLOADING.name)
    }

    override suspend fun markDownloaded(modelId: String, localPath: String, version: String) {
        val model = modelDao.getById(modelId) ?: return
        modelDao.markDownloaded(
            id = modelId,
            status = DownloadStatus.DOWNLOADED.name,
            localPath = localPath,
            version = version,
        )
        if (modelDao.getActiveByType(model.type) == null) {
            modelDao.clearActiveForType(model.type)
            modelDao.setActive(modelId)
        }
    }

    override suspend fun markError(modelId: String) {
        modelDao.updateDownloadStatus(modelId, DownloadStatus.ERROR.name)
    }

    override suspend fun deleteModel(modelId: String) {
        val model = modelDao.getById(modelId) ?: return
        val builtInSeed = catalog.seedFor(modelId)
        model.localPath
            ?.takeIf { it.isNotBlank() && !it.startsWith("bundled://") }
            ?.let { path -> File(path).takeIf { it.exists() }?.delete() }

        if (builtInSeed?.downloadedByDefault == true) {
            modelDao.markDownloaded(
                id = modelId,
                status = DownloadStatus.DOWNLOADED.name,
                localPath = "bundled://$modelId",
                version = builtInSeed.version ?: "bundled-preview",
            )
            return
        }

        modelDao.clearLocalModel(modelId, DownloadStatus.NOT_DOWNLOADED.name)

        if (model.isActive) {
            modelDao.firstDownloadedByType(
                type = model.type,
                downloadedStatus = DownloadStatus.DOWNLOADED.name,
                excludedId = modelId,
            )?.let { fallback ->
                modelDao.clearActiveForType(model.type)
                modelDao.setActive(fallback.id)
            }
        }
    }

    override suspend fun setActiveModel(modelId: String) {
        val model = modelDao.getById(modelId) ?: return
        if (model.downloadStatus != DownloadStatus.DOWNLOADED.name) return
        modelDao.clearActiveForType(model.type)
        modelDao.setActive(modelId)
    }
}
