package com.fluffnark.motoringdashboard.data

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
