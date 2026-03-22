package com.localai.hub.core.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "models")
data class ModelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val format: String,
    val engine: String,
    val sizeMb: Int,
    val languageSupport: String,
    val minRamGb: Int,
    val supportsGpu: Boolean,
    val supportsNpu: Boolean,
    val description: String,
    val downloadStatus: String,
    val version: String?,
    val localPath: String?,
    val isActive: Boolean,
)

