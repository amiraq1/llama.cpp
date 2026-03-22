package com.localai.hub.core.storage

import com.localai.hub.core.modelregistry.DownloadStatus
import com.localai.hub.core.modelregistry.LocalModel
import com.localai.hub.core.modelregistry.ModelFormat
import com.localai.hub.core.modelregistry.ModelSeed
import com.localai.hub.core.modelregistry.ModelType
import com.localai.hub.core.modelregistry.RuntimeEngine

fun ModelEntity.toDomain(): LocalModel {
    return LocalModel(
        id = id,
        name = name,
        type = ModelType.valueOf(type),
        format = ModelFormat.valueOf(format),
        engine = RuntimeEngine.valueOf(engine),
        sizeMb = sizeMb,
        languageSupport = languageSupport.split(",").filter(String::isNotBlank),
        minRamGb = minRamGb,
        supportsGpu = supportsGpu,
        supportsNpu = supportsNpu,
        description = description,
        downloadStatus = DownloadStatus.valueOf(downloadStatus),
        version = version,
        localPath = localPath,
        isActive = isActive,
    )
}

fun ModelSeed.toEntity(): ModelEntity {
    return ModelEntity(
        id = id,
        name = name,
        type = type.name,
        format = format.name,
        engine = engine.name,
        sizeMb = sizeMb,
        languageSupport = languageSupport.joinToString(","),
        minRamGb = minRamGb,
        supportsGpu = supportsGpu,
        supportsNpu = supportsNpu,
        description = description,
        downloadStatus = if (downloadedByDefault) {
            DownloadStatus.DOWNLOADED.name
        } else {
            DownloadStatus.NOT_DOWNLOADED.name
        },
        version = version,
        localPath = if (downloadedByDefault) "bundled://$id" else null,
        isActive = activeByDefault,
    )
}

