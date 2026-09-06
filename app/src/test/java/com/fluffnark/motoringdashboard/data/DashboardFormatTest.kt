package com.fluffnark.motoringdashboard.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardFormatTest {
    @Test fun convertsMetersPerSecondToMilesPerHour() {
        assertEquals(60f, DashboardFormat.metersPerSecondToMph(26.8224f), 0.01f)
    }

    @Test fun convertsMetersToMiles() {
        assertEquals(100f, DashboardFormat.metersToMiles(160_934.4f), 0.01f)
    }

    @Test fun convertsKilometersToMiles() {
        assertEquals(100f, DashboardFormat.kilometersToMiles(160.9344f), 0.01f)
    }

    @Test fun formatsMissingReadingsWithoutInventingData() {
        assertEquals("—", DashboardFormat.speed(null))
        assertEquals("—", DashboardFormat.percent(null))
        assertEquals("—", DashboardFormat.miles(null))
    }

    @Test fun clampsImpossibleDashboardValues() {
        assertEquals("0", DashboardFormat.speed(-10f))
        assertEquals("100%", DashboardFormat.percent(140f))
        assertEquals("0 mi", DashboardFormat.miles(-5f))
    }
}
