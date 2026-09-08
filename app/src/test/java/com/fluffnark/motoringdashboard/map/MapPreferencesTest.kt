package com.fluffnark.motoringdashboard.map

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MapPreferencesTest {
    @Test fun selectedCartographyPersists() {
        val context = RuntimeEnvironment.getApplication()
        MapPreferences.setLook(context, MapStyle.Look.SCANDINAVIAN)
        assertEquals(MapStyle.Look.SCANDINAVIAN, MapPreferences.getLook(context))
    }
}
