package com.localai.hub.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {
    @Query("SELECT COUNT(*) FROM models")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(models: List<ModelEntity>)

    @Query("SELECT * FROM models ORDER BY isActive DESC, type ASC, name ASC")
    fun observeAll(): Flow<List<ModelEntity>>

    @Query("SELECT * FROM models WHERE type = :type AND isActive = 1 LIMIT 1")
    fun observeActiveByType(type: String): Flow<ModelEntity?>

    @Query("SELECT * FROM models WHERE type = :type AND isActive = 1 LIMIT 1")
    suspend fun getActiveByType(type: String): ModelEntity?

    @Query("SELECT * FROM models WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ModelEntity?

    @Query("UPDATE models SET downloadStatus = :status WHERE id = :id")
    suspend fun updateDownloadStatus(id: String, status: String)

    @Query(
        "UPDATE models SET downloadStatus = :status, localPath = :localPath, version = :version " +
            "WHERE id = :id",
    )
    suspend fun markDownloaded(
        id: String,
        status: String,
        localPath: String,
        version: String,
    )

    @Query(
        "UPDATE models SET downloadStatus = :status, localPath = NULL, version = NULL, isActive = 0 " +
            "WHERE id = :id",
    )
    suspend fun clearLocalModel(id: String, status: String)

    @Query("UPDATE models SET isActive = 0 WHERE type = :type")
    suspend fun clearActiveForType(type: String)

    @Query("UPDATE models SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: String)

    @Query(
        "SELECT * FROM models WHERE type = :type AND downloadStatus = :downloadedStatus AND id != :excludedId " +
            "ORDER BY name ASC LIMIT 1",
    )
    suspend fun firstDownloadedByType(
        type: String,
        downloadedStatus: String,
        excludedId: String,
    ): ModelEntity?
}

