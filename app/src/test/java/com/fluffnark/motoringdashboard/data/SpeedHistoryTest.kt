package com.fluffnark.motoringdashboard.data

import org.junit.Assert.*
import org.junit.Test

class SpeedHistoryTest {
    @Test fun sensorBurstsStayBoundedAndExpire() {
        val history = SpeedHistory()
        var samples = emptyList<SpeedSample>()
        for (time in 0L..300_000L step 50L) samples = history.record(42f, time)
        assertTrue(samples.size <= 121)
        assertTrue(samples.first().elapsedMillis >= 180_000L)
        assertEquals(42f, samples.last().mph)
    }

    @Test fun missingAndInvalidDataLeaveGaps() {
        val history = SpeedHistory()
        history.record(0f, 0)
        history.record(null, 1_000)
        history.record(Float.NaN, 2_000)
        val samples = history.record(-1f, 3_000)
        assertEquals(0f, samples.first().mph)
        assertTrue(samples.drop(1).all { it.mph == null })
    }

    @Test fun resetDoesNotReusePreviousTrip() {
        val history = SpeedHistory()
        history.record(60f, 10_000)
        history.clear()
        assertEquals(listOf(SpeedSample(20_000, 0f)), history.record(0f, 20_000))
    }
}
