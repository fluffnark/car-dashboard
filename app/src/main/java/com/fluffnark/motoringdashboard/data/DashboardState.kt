package com.fluffnark.motoringdashboard.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DashboardState(
    val speedMph: Float? = null,
    val rawSpeedMph: Float? = null,
    val fuelPercent: Float? = null,
    val rangeMiles: Float? = null,
    val odometerMiles: Float? = null,
    val vehicleName: String? = null,
    val connected: Boolean = false,
    val speedHistory: List<SpeedSample> = emptyList(),
)

object DashboardRepository {
    private val history = SpeedHistory()
    private val mutableState = MutableStateFlow(DashboardState())
    val state = mutableState.asStateFlow()

    fun update(block: (DashboardState) -> DashboardState) {
        mutableState.value = block(mutableState.value)
    }

    fun setConnected(connected: Boolean) = update { it.copy(connected = connected) }

    fun recordSpeed(displayMph: Float?, rawMph: Float?, elapsedMillis: Long) {
        val display = displayMph?.takeIf { it.isFinite() && it >= 0 }
        val raw = rawMph?.takeIf { it.isFinite() && it >= 0 }
        update { it.copy(speedMph = display, rawSpeedMph = raw,
            speedHistory = history.record(display ?: raw, elapsedMillis)) }
    }

    fun reset(connected: Boolean = false) {
        history.clear()
        update { DashboardState(connected = connected) }
    }
}

object DashboardFormat {
    private const val MPS_TO_MPH = 2.2369363f
    private const val METERS_TO_MILES = 0.0006213712f

    fun metersPerSecondToMph(value: Float): Float = value * MPS_TO_MPH
    fun metersToMiles(value: Float): Float = value * METERS_TO_MILES
    fun kilometersToMiles(value: Float): Float = value * 0.6213712f
    fun speed(value: Float?): String = value?.takeIf { it.isFinite() }?.let { "%.0f".format(it.coerceAtLeast(0f)) } ?: "—"
    fun percent(value: Float?): String = value?.takeIf { it.isFinite() }?.let { "%.0f%%".format(it.coerceIn(0f, 100f)) } ?: "—"
    fun miles(value: Float?): String = value?.takeIf { it.isFinite() }?.let { "%,.0f mi".format(it.coerceAtLeast(0f)) } ?: "—"
}
