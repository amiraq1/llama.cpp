package com.localai.hub.core.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.localai.hub.core.modelregistry.BuiltInModelCatalog
import com.localai.hub.core.modelregistry.ModelSeed
import com.localai.hub.core.storage.ModelRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

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
        val catalog = entryPoint.catalog()
        val seed = catalog.seedFor(modelId)

        if (seed?.downloadUrl.isNullOrBlank()) {
            repository.markError(modelId)
            return Result.failure()
        }

        repository.setDownloading(modelId)

        return runCatching {
            val localFile = downloadToInternalStorage(seed!!)
            repository.markDownloaded(
                modelId = modelId,
                localPath = localFile.absolutePath,
                version = localFile.name,
            )
            Result.success()
        }.getOrElse {
            repository.markError(modelId)
            if (it is IOException) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun downloadToInternalStorage(seed: ModelSeed): File {
        val targetName = seed.downloadFileName ?: seed.downloadUrl!!.substringAfterLast('/')
        val finalFile = File(applicationContext.filesDir, "models/${seed.id}-$targetName")
        val partialFile = File("${finalFile.absolutePath}.part").apply {
            parentFile?.mkdirs()
        }

        if (finalFile.exists() && verifyChecksum(finalFile, seed.downloadSha256)) {
            return finalFile
        }

        val existingLength = partialFile.takeIf(File::exists)?.length() ?: 0L
        val connection = openConnection(seed.downloadUrl!!, existingLength)
        val responseCode = connection.responseCode
        val append = responseCode == HttpURLConnection.HTTP_PARTIAL
        val outputFile = if (append) partialFile else partialFile.also { if (it.exists()) it.delete() }

        if (responseCode !in setOf(HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_PARTIAL)) {
            connection.disconnect()
            throw IllegalStateException("HTTP ${connection.responseCode} while downloading ${seed.id}")
        }

        val totalBytes = resolveExpectedLength(connection, existingLength, append)
        connection.inputStream.use { input ->
            FileOutputStream(outputFile, append).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var downloadedBytes = if (append) existingLength else 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    downloadedBytes += read
                    if (totalBytes > 0L) {
                        val progress = ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                        setProgressAsync(workDataOf(KEY_PROGRESS to progress))
                    }
                }
                output.flush()
            }
        }
        connection.disconnect()

        if (!verifyChecksum(outputFile, seed.downloadSha256)) {
            outputFile.delete()
            throw IllegalStateException("Checksum mismatch for ${seed.id}")
        }

        if (finalFile.exists()) {
            finalFile.delete()
        }
        if (!outputFile.renameTo(finalFile)) {
            throw IllegalStateException("Failed to move downloaded asset for ${seed.id}")
        }

        return finalFile
    }

    private fun openConnection(url: String, existingLength: Long): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 20_000
            readTimeout = 30_000
            requestMethod = "GET"
            setRequestProperty("Accept-Encoding", "identity")
            if (existingLength > 0L) {
                setRequestProperty("Range", "bytes=$existingLength-")
            }
            connect()
        }
    }

    private fun resolveExpectedLength(
        connection: HttpURLConnection,
        existingLength: Long,
        append: Boolean,
    ): Long {
        val contentLength = connection.getHeaderFieldLong("Content-Length", -1L)
        return when {
            append && contentLength > 0L -> existingLength + contentLength
            else -> contentLength
        }
    }

    private fun verifyChecksum(file: File, expectedSha256: String?): Boolean {
        if (expectedSha256.isNullOrBlank()) {
            return file.exists() && file.length() > 0L
        }
        if (!file.exists()) {
            return false
        }

        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }

        val actualHash = digest.digest().joinToString(separator = "") { byte ->
            "%02X".format(byte)
        }
        return actualHash.equals(expectedSha256, ignoreCase = true)
    }

    companion object {
        const val KEY_MODEL_ID = "model_id"
        const val KEY_PROGRESS = "progress"

        fun uniqueWorkName(modelId: String): String = "download-model-$modelId"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ModelDownloadWorkerEntryPoint {
    fun modelRepository(): ModelRepository
    fun catalog(): BuiltInModelCatalog
}
