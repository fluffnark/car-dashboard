package com.fluffnark.motoringdashboard.map

import android.graphics.Bitmap
import android.graphics.Canvas
import com.fluffnark.motoringdashboard.data.VehicleData
import com.fluffnark.motoringdashboard.debug.SimulationRoute
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DashboardRendererTest {
    @Test fun rendersDistinctDayAndNightInstrumentFaces() {
        val renderer = DashboardRenderer(RuntimeEnvironment.getApplication())
        repeat(48) { renderer.record(SimulationRoute.sample(it)) }
        val telemetry = SimulationRoute.sample(48)
        val vehicle = VehicleData(speedMph = 58f, fuelPercent = 64f, rangeMiles = 226f,
            odometerMiles = 48_312f)
        val day = render(renderer, telemetry, vehicle, false)
        val night = render(renderer, telemetry, vehicle, true)

        assertNotEquals(day.getPixel(0, 0), night.getPixel(0, 0))
        assertNotEquals(day.getPixel(85, 207), day.getPixel(0, 0))

        if (System.getenv("RECORD_SCREENSHOTS") == "1") {
            val directory = File(requireNotNull(System.getProperty("user.dir"))).resolve("../docs/screenshots")
            directory.mkdirs()
            directory.resolve("android-auto-day.png").outputStream().use {
                day.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            directory.resolve("android-auto-night.png").outputStream().use {
                night.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    private fun render(
        renderer: DashboardRenderer,
        telemetry: com.fluffnark.motoringdashboard.data.DriveTelemetry,
        vehicle: VehicleData,
        night: Boolean,
    ) = Bitmap.createBitmap(941, 423, Bitmap.Config.ARGB_8888).also {
        renderer.draw(Canvas(it), telemetry, vehicle, night, LocalTime.of(10, 9))
    }
}
