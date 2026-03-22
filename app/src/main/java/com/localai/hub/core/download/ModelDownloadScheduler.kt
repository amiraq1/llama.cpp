package com.localai.hub.core.download

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.localai.hub.core.storage.ModelRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelDownloadScheduler @Inject constructor(
    private val workManager: WorkManager,
    private val repository: ModelRepository,
) {
    suspend fun enqueue(modelId: String) {
        repository.queueDownload(modelId)
        val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(workDataOf(ModelDownloadWorker.KEY_MODEL_ID to modelId))
            .build()
        workManager.enqueueUniqueWork(
            ModelDownloadWorker.uniqueWorkName(modelId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(modelId: String) {
        workManager.cancelUniqueWork(ModelDownloadWorker.uniqueWorkName(modelId))
    }
}

