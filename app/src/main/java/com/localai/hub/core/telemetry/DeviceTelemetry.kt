package com.localai.hub.core.telemetry

data class DeviceTelemetry(
    val freeRamMb: Int,
    val totalRamMb: Int,
    val thermalStatus: String,
    val batterySaverEnabled: Boolean,
)

