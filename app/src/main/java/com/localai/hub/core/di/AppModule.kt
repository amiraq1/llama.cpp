package com.localai.hub.core.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.localai.hub.core.inference.InferenceEngine
import com.localai.hub.core.inference.LiteRtEngine
import com.localai.hub.core.inference.LlamaCppEngine
import com.localai.hub.core.inference.OnnxRuntimeEngine
import com.localai.hub.core.storage.AppDatabase
import com.localai.hub.core.storage.ModelDao
import com.localai.hub.core.storage.ModelRepository
import com.localai.hub.core.storage.OfflineModelRepository
import com.localai.hub.core.telemetry.AndroidTelemetryMonitor
import com.localai.hub.core.telemetry.TelemetryMonitor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "local-ai-hub.db",
        ).fallbackToDestructiveMigration(true).build()
    }

    @Provides
    fun provideModelDao(database: AppDatabase): ModelDao = database.modelDao()

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context,
    ): WorkManager = WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideInferenceEngines(
        llamaCppEngine: LlamaCppEngine,
        liteRtEngine: LiteRtEngine,
        onnxRuntimeEngine: OnnxRuntimeEngine,
    ): List<InferenceEngine> {
        return listOf(llamaCppEngine, liteRtEngine, onnxRuntimeEngine)
    }

    @Provides
    @Singleton
    fun provideTelemetryMonitor(
        monitor: AndroidTelemetryMonitor,
    ): TelemetryMonitor = monitor
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindModelRepository(
        repository: OfflineModelRepository,
    ): ModelRepository
}
