package com.localai.hub.core.telemetry

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

@Singleton
class AndroidTelemetryMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) : TelemetryMonitor {
    override fun stream(): Flow<DeviceTelemetry> = flow {
        while (currentCoroutineContext().isActive) {
            emit(snapshot())
            delay(5_000L)
        }
    }

    private fun snapshot(): DeviceTelemetry {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)

        val thermalStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when (powerManager.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "مستقر"
                PowerManager.THERMAL_STATUS_LIGHT -> "دافئ"
                PowerManager.THERMAL_STATUS_MODERATE -> "مرتفع"
                PowerManager.THERMAL_STATUS_SEVERE -> "ساخن"
                PowerManager.THERMAL_STATUS_CRITICAL -> "حرج"
                PowerManager.THERMAL_STATUS_EMERGENCY -> "طوارئ"
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "إيقاف"
                else -> "غير معروف"
            }
        } else {
            "غير مدعوم"
        }

        return DeviceTelemetry(
            freeRamMb = (memoryInfo.availMem / 1024 / 1024).toInt(),
            totalRamMb = (memoryInfo.totalMem / 1024 / 1024).toInt(),
            thermalStatus = thermalStatus,
            batterySaverEnabled = powerManager.isPowerSaveMode,
        )
    }
}

