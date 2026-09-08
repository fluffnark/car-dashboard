package com.fluffnark.motoringdashboard.data

data class DriveTelemetry(
    val latitude: Double,
    val longitude: Double,
    val speedMph: Float,
    val headingDegrees: Float,
    val elevationFeet: Float?,
    val tripMiles: Float,
    val gradePercent: Float?,
    val road: String,
    val accuracyFeet: Float? = null,
    val demo: Boolean = false,
)

object TelemetryFormat {
    private val points = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    fun compass(degrees: Float): String {
        val normalized = ((degrees % 360f) + 360f) % 360f
        return points[((normalized + 11.25f) / 22.5f).toInt() % 16]
    }
    fun heading(degrees: Float): String = "${compass(degrees)} ${(((degrees % 360f) + 360f) % 360f).toInt()}°"
    fun feet(value: Float?): String = value?.let { "%,.0f ft".format(it) } ?: "Elevation —"
    fun grade(value: Float?): String = value?.let { "%+.1f%%".format(it) } ?: "—"
}
