package com.localai.hub.core.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.localai.hub.core.storage.ModelRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File
import kotlinx.coroutines.delay

class ModelDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val modelId = inputData.getString(KEY_MODEL_ID) ?: return Result.failure()
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ModelDownloadWorkerEntryPoint::class.java,
        )
        val repository = entryPoint.modelRepository()
        repository.setDownloading(modelId)
        delay(1_500L)

        val modelFile = File(applicationContext.filesDir, "models/$modelId.bin").apply {
            parentFile?.mkdirs()
            writeText("Local AI Hub placeholder file for $modelId")
        }

        repository.markDownloaded(
            modelId = modelId,
            localPath = modelFile.absolutePath,
            version = "mvp-download-1",
        )
        return Result.success()
    }

    companion object {
        const val KEY_MODEL_ID = "model_id"

        fun uniqueWorkName(modelId: String): String = "download-model-$modelId"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ModelDownloadWorkerEntryPoint {
    fun modelRepository(): ModelRepository
}
