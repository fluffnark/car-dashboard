package com.fluffnark.motoringdashboard.map

import android.app.Application
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.car.app.testing.TestCarContext
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MapScreenTest {
    @Test fun mapTemplateHasCompactTelemetryAndRotaryControls() {
        val context = TestCarContext.createCarContext(RuntimeEnvironment.getApplication() as Application)
        val template = MapScreen(context).onGetTemplate() as MapWithContentTemplate

        assertNotNull(template.contentTemplate)
        assertEquals(3, template.actionStrip!!.actions.size)
        assertEquals(4, template.mapController!!.mapActionStrip!!.actions.size)
        assertEquals(androidx.car.app.model.Action.PAN, template.mapController!!.mapActionStrip!!.actions.first())
    }
}
