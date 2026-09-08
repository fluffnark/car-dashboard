package com.fluffnark.motoringdashboard.poi

import org.junit.Assert.*
import org.junit.Test

class PoiRepositoryTest {
    @Test fun nearbyPlacesAreDistanceSortedAndLimited() {
        val places = PoiRepository.nearby(38.0228, -107.6714, 3)
        assertEquals(3, places.size)
        assertTrue(places.zipWithNext().all { (a, b) -> a.second <= b.second })
        assertTrue(places.all { it.second < 5 })
    }
    @Test fun distanceCalculationIsReasonable() {
        assertEquals(69.0, PoiRepository.miles(0.0, 0.0, 0.0, 1.0), 0.2)
    }
    @Test fun regionalGuideDoesNotPretendToBeNearbyElsewhere() {
        assertTrue(PoiRepository.nearby(40.7128, -74.0060).isEmpty())
    }
}
