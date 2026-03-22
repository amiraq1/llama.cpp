package com.localai.hub.core.telemetry

import kotlinx.coroutines.flow.Flow

interface TelemetryMonitor {
    fun stream(): Flow<DeviceTelemetry>
}

