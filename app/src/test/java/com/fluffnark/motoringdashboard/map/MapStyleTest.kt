package com.fluffnark.motoringdashboard.map

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MapStyleTest {
    @Test fun allCartographyVariantsAreValidStyleDocuments() {
        MapStyle.Look.entries.forEach { look ->
            listOf(false, true).forEach { night ->
                val style = JSONObject(MapStyle.json(night, look))
                assertEquals(8, style.getInt("version"))
                assertTrue(style.getJSONArray("layers").length() >= 7)
                assertTrue(style.getJSONObject("sources").has("streets"))
            }
        }
    }

    @Test fun dayAndNightPalettesAreDistinct() {
        assertNotEquals(MapStyle.json(false), MapStyle.json(true))
    }
}
