package com.fluffnark.motoringdashboard.car

import android.app.Application
import android.content.Intent
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.SectionedItemTemplate
import androidx.car.app.model.GridItem
import androidx.car.app.model.Section
import androidx.car.app.OnDoneCallback
import androidx.car.app.serialization.Bundleable
import androidx.car.app.testing.SessionController
import androidx.car.app.testing.TestCarContext
import androidx.car.app.testing.TestScreenManager
import androidx.lifecycle.Lifecycle
import com.fluffnark.motoringdashboard.data.DashboardRepository
import com.fluffnark.motoringdashboard.data.DashboardState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DashboardScreenTest {
    private val Section<*>.items: List<GridItem>
        get() {
            var result: List<GridItem>? = null
            itemsDelegate.requestItemRange(0, itemsDelegate.size - 1, object : OnDoneCallback {
                override fun onSuccess(response: Bundleable?) {
                    @Suppress("UNCHECKED_CAST")
                    result = response!!.get() as List<GridItem>
                }
                override fun onFailure(response: Bundleable) {
                    throw AssertionError("Unable to read grid items: ${response.get()}")
                }
            })
            shadowOf(android.os.Looper.getMainLooper()).idle()
            return requireNotNull(result)
        }
    private lateinit var carContext: TestCarContext
    private lateinit var screen: DashboardScreen
    private lateinit var sessionController: SessionController

    @Before
    fun setUp() {
        val application = RuntimeEnvironment.getApplication() as Application
        carContext = TestCarContext.createCarContext(application)
        sessionController = SessionController(DashboardSession(), carContext, Intent())
        sessionController.moveToState(Lifecycle.State.CREATED)
        screen = DashboardScreen(carContext)
        DashboardRepository.update { DashboardState() }
    }

    @Test
    fun initialTemplateRendersBeforeVehiclePermissionsAreReady() {
        val template = screen.onGetTemplate() as SectionedItemTemplate

        assertEquals("Instruments", template.header?.title.toString())
        assertEquals(null, template.header?.startHeaderAction)
        assertEquals(3, template.sections.single().items.size)
        template.sections.single().items.forEach { item ->
            assertNotNull((item as GridItem).image)
            assertNotNull(item.onClickDelegate)
        }
    }

    @Test
    fun everyDashboardRowHasVisiblePrimaryAndSecondaryText() {
        val pane = (DashboardScreen(carContext, showDetails = true).onGetTemplate() as PaneTemplate).pane

        pane.rows.forEach { row ->
            assertTrue(row.title.toString().isNotBlank())
            assertTrue(row.texts.isNotEmpty())
            assertTrue(row.texts.first().toString().isNotBlank())
        }
        assertNotNull(pane.actions.single().onClickDelegate)
    }

    @Test
    fun screenStartsAndStopsWithoutCarPermissionsOrHardware() {
        sessionController.moveToState(Lifecycle.State.STARTED)

        assertTrue(screen.onGetTemplate() is SectionedItemTemplate)

        sessionController.moveToState(Lifecycle.State.DESTROYED)
    }

    @Test
    fun permissionActionIsHiddenWhenVehiclePermissionsAreGranted() {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(
            "com.google.android.gms.permission.CAR_SPEED",
            "com.google.android.gms.permission.CAR_FUEL",
            "com.google.android.gms.permission.CAR_MILEAGE",
        )

        val pane = (DashboardScreen(carContext, showDetails = true).onGetTemplate() as PaneTemplate).pane

        assertTrue(pane.actions.isEmpty())
    }

    @Test
    fun liveReadingsPreserveTitlesForHostRefreshAndFallBackToRawSpeed() {
        val before = screen.onGetTemplate() as SectionedItemTemplate
        DashboardRepository.update { it.copy(rawSpeedMph = 42f, fuelPercent = 65f, rangeMiles = 200f) }
        val after = screen.onGetTemplate() as SectionedItemTemplate

        assertEquals(before.header!!.title, after.header!!.title)
        assertEquals(before.sections.single().items.map { (it as GridItem).title },
            after.sections.single().items.map { (it as GridItem).title })
        assertEquals("42 mph", (after.sections.single().items[1] as GridItem).text.toString())
        assertEquals("65% · 200 mi", (after.sections.single().items[2] as GridItem).text.toString())
    }

    @Test
    fun partialPermissionsStillOfferRemainingAccess() {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions("com.google.android.gms.permission.CAR_SPEED")
        val pane = (DashboardScreen(carContext, showDetails = true).onGetTemplate() as PaneTemplate).pane
        assertEquals(1, pane.actions.size)
    }

    @Test
    fun disconnectClearsVehicleReadings() {
        sessionController.moveToState(Lifecycle.State.STARTED)
        DashboardRepository.update { it.copy(speedMph = 42f, fuelPercent = 65f, connected = true) }

        sessionController.moveToState(Lifecycle.State.DESTROYED)

        assertEquals(DashboardState(), DashboardRepository.state.value)
    }
}
