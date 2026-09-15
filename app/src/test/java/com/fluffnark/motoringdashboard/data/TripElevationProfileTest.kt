package com.fluffnark.motoringdashboard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripElevationProfileTest {
    @Test fun retainsTheWholeTripBeyondTheOldSeventyTwoSampleWindow() {
        val profile = TripElevationProfile()
        repeat(500) { index -> profile.record(sample(index / 10f, 5_000f + index)) }

        val points = profile.snapshot()
        assertEquals(0f, points.first().distanceMiles)
        assertEquals(49.9f, points.last().distanceMiles)
        assertTrue(points.size > 72)
    }

    @Test fun boundsMemoryWithoutLosingTripEndpoints() {
        val profile = TripElevationProfile(maxSamples = 32)
        repeat(300) { index -> profile.record(sample(index / 10f, 5_000f + index)) }

        val points = profile.snapshot()
        assertTrue(points.size <= 32)
        assertEquals(0f, points.first().distanceMiles)
        assertEquals(29.9f, points.last().distanceMiles)
    }

    @Test fun clearsWhenANewTripStarts() {
        val profile = TripElevationProfile()
        profile.record(sample(8f, 7_000f))
        profile.record(sample(0f, 5_000f))

        assertEquals(listOf(ElevationSample(0f, 5_000f)), profile.snapshot())
    }

    private fun sample(distance: Float, elevation: Float) = DriveTelemetry(
        0.0, 0.0, 0f, 0f, elevation, distance, null, "",
    )
}
