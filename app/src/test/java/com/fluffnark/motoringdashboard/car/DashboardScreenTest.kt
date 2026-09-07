package com.fluffnark.motoringdashboard.car

import android.app.Application
import android.content.Intent
import androidx.car.app.model.PaneTemplate
import androidx.car.app.testing.SessionController
import androidx.car.app.testing.TestCarContext
import androidx.car.app.testing.TestScreenManager
import androidx.lifecycle.Lifecycle
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
    private lateinit var carContext: TestCarContext
    private lateinit var screen: DashboardScreen
    private lateinit var sessionController: SessionController

    @Before
    fun setUp() {
        val application = RuntimeEnvironment.getApplication() as Application
        carContext = TestCarContext.createCarContext(application)
        sessionController = SessionController(DashboardSession(), carContext, Intent())
        sessionController.moveToState(Lifecycle.State.CREATED)
        screen = carContext.getCarService(TestScreenManager::class.java).top as DashboardScreen
    }

    @Test
    fun initialTemplateRendersBeforeVehiclePermissionsAreReady() {
        val template = screen.onGetTemplate() as PaneTemplate

        assertTrue(template.header?.title.toString().contains("·"))
        assertEquals(null, template.header?.startHeaderAction)
        assertTrue(template.header?.endHeaderActions.orEmpty().isEmpty())
        assertEquals(3, template.pane.rows.size)
        assertEquals(1, template.pane.actions.size)
    }

    @Test
    fun everyDashboardRowHasVisiblePrimaryAndSecondaryText() {
        val pane = (screen.onGetTemplate() as PaneTemplate).pane

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

        assertTrue(screen.onGetTemplate() is PaneTemplate)

        sessionController.moveToState(Lifecycle.State.DESTROYED)
    }

    @Test
    fun permissionActionIsHiddenWhenVehiclePermissionsAreGranted() {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(
            "com.google.android.gms.permission.CAR_SPEED",
            "com.google.android.gms.permission.CAR_FUEL",
            "com.google.android.gms.permission.CAR_MILEAGE",
        )

        val pane = (screen.onGetTemplate() as PaneTemplate).pane

        assertTrue(pane.actions.isEmpty())
    }
}
