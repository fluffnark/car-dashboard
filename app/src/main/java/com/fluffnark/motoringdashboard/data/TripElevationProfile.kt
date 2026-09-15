package com.fluffnark.motoringdashboard.data

import kotlin.math.abs

data class ElevationSample(val distanceMiles: Float, val elevationFeet: Float)

/** Keeps the complete trip shape while bounding memory through adaptive decimation. */
class TripElevationProfile(private val maxSamples: Int = 4_096) {
    private val samples = ArrayList<ElevationSample>()
    private var lastTripMiles: Float? = null

    @Synchronized
    fun record(telemetry: DriveTelemetry) {
        val distance = telemetry.tripMiles.coerceAtLeast(0f)
        if (lastTripMiles?.let { distance + RESET_TOLERANCE_MILES < it } == true) {
            samples.clear()
        }
        lastTripMiles = distance
        val elevation = telemetry.elevationFeet ?: return
        val sample = ElevationSample(distance, elevation)
        val previous = samples.lastOrNull()
        when {
            previous == null -> samples += sample
            distance - previous.distanceMiles >= MIN_SAMPLE_DISTANCE_MILES -> samples += sample
            abs(elevation - previous.elevationFeet) >= MIN_ELEVATION_CHANGE_FEET -> samples += sample
            else -> samples[samples.lastIndex] = sample
        }
        if (samples.size > maxSamples) decimate()
    }

    @Synchronized
    fun snapshot(): List<ElevationSample> = samples.toList()

    private fun decimate() {
        val reduced = ArrayList<ElevationSample>(samples.size / 2 + 2)
        reduced += samples.first()
        var index = 1
        while (index < samples.lastIndex) {
            val first = samples[index]
            val second = samples[minOf(index + 1, samples.lastIndex - 1)]
            val anchor = reduced.last().elevationFeet
            reduced += if (abs(first.elevationFeet - anchor) >= abs(second.elevationFeet - anchor)) first else second
            index += 2
        }
        if (reduced.last() != samples.last()) reduced += samples.last()
        samples.clear()
        samples.addAll(reduced)
    }

    private companion object {
        const val MIN_SAMPLE_DISTANCE_MILES = .01f
        const val MIN_ELEVATION_CHANGE_FEET = 6f
        const val RESET_TOLERANCE_MILES = .05f
    }
}
