package com.fluffnark.motoringdashboard.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TelemetryFormatTest {
    @Test fun compassWrapsAndUsesEightDirections() {
        assertEquals("N", TelemetryFormat.compass(0f))
        assertEquals("NE", TelemetryFormat.compass(44f))
        assertEquals("W", TelemetryFormat.compass(-90f))
        assertEquals("N", TelemetryFormat.compass(359f))
    }
    @Test fun headingAndElevationAreAutomotiveReadable() {
        assertEquals("NNW 338°", TelemetryFormat.heading(338.9f))
        assertEquals("7,315 ft", TelemetryFormat.feet(7315f))
        assertEquals("+4.2%", TelemetryFormat.grade(4.2f))
        assertEquals("Elevation —", TelemetryFormat.feet(null))
        assertEquals("—", TelemetryFormat.grade(null))
    }
}
