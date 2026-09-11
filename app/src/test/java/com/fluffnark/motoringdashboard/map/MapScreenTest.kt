package com.fluffnark.motoringdashboard.map

import android.app.Application
import androidx.car.app.OnDoneCallback
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.PaneTemplate
import androidx.car.app.testing.TestCarContext
import com.fluffnark.motoringdashboard.trip.TripListRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MapScreenTest {
    private val application get() = RuntimeEnvironment.getApplication() as Application

    @Before fun clearTripList() {
        application.getSharedPreferences("trip_list", 0).edit().clear().commit()
    }

    @Test fun mapTemplateHasCompactTelemetryAndPersistentHostControls() {
        val context = TestCarContext.createCarContext(application)
        val template = MapScreen(context).onGetTemplate() as MapWithContentTemplate

        assertNotNull(template.contentTemplate)
        assertTrue(template.contentTemplate is PaneTemplate)
        assertEquals(1, (template.contentTemplate as PaneTemplate).pane.rows.size)
        assertEquals(1, template.actionStrip!!.actions.size)
        assertNull(template.mapController)
        template.actionStrip!!.actions.forEach {
            assertEquals(androidx.car.app.model.Action.FLAG_IS_PERSISTENT, it.flags)
            assertNotNull(it.icon)
        }
    }

    @Test fun persistentActionsExposeRotaryClickableDelegates() {
        val context = TestCarContext.createCarContext(application)
        TripListRepository(context).add("Pick up coffee")
        val screen = MapScreen(context)
        val actions = (screen.onGetTemplate() as MapWithContentTemplate).actionStrip!!.actions

        assertFalse(actions[0].onClickDelegate!!.isParkedOnly)
        assertTrue(actions[1].onClickDelegate!!.isParkedOnly)
        actions[0].onClickDelegate!!.sendClick(object : OnDoneCallback {})

        val updated = screen.onGetTemplate() as MapWithContentTemplate
        assertTrue(updated.contentTemplate is ListTemplate)
    }
}
