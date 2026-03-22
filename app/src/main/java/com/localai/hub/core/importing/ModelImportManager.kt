package com.localai.hub.core.importing

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.arm.aichat.gguf.GgufMetadataReader
import com.localai.hub.core.modelregistry.LocalModel
import com.localai.hub.core.modelregistry.ModelFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class ModelImportManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val ggufReader = GgufMetadataReader.create()

    suspend fun importModel(
        model: LocalModel,
        uri: Uri,
    ): ImportedModel = withContext(Dispatchers.IO) {
        require(model.format == ModelFormat.GGUF) {
            "الاستيراد المحلي مفعّل حاليًا لنماذج GGUF فقط."
        }

        val targetFile = File(context.filesDir, "models/${model.id}.gguf")
        targetFile.parentFile?.mkdirs()

        val isValidGguf = ggufReader.ensureSourceFileFormat(context, uri)
        if (!isValidGguf) {
            throw IOException("الملف المحدد ليس GGUF صالحًا.")
        }

        context.contentResolver.openInputStream(uri)?.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("تعذر فتح الملف المحدد.")

        ImportedModel(
            absolutePath = targetFile.absolutePath,
            version = resolveDisplayName(uri) ?: targetFile.name,
        )
    }

    private fun resolveDisplayName(uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            cursor.firstStringOrNull(OpenableColumns.DISPLAY_NAME)
        }
    }
}

data class ImportedModel(
    val absolutePath: String,
    val version: String,
)

private fun Cursor.firstStringOrNull(columnName: String): String? {
    if (!moveToFirst()) return null
    val columnIndex = getColumnIndex(columnName)
    if (columnIndex < 0 || isNull(columnIndex)) return null
    return getString(columnIndex)
}
