package com.fluffnark.motoringdashboard.map

import android.graphics.Rect
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.Surface
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import com.fluffnark.motoringdashboard.data.DriveTelemetry
import com.fluffnark.motoringdashboard.data.VehicleData

/** Draws one lightweight Canvas frame at a time; no map engine, virtual display, or animation loop. */
class DashboardSurface(context: CarContext) : SurfaceCallback {
    private val renderer = DashboardRenderer(context)
    private val thread = HandlerThread("MotoringDashboardRender").apply { start() }
    private val handler = Handler(thread.looper)
    private val lock = Any()
    private var target: Surface? = null
    private var telemetry: DriveTelemetry? = null
    private var vehicle = VehicleData()
    private var lastFrameAt = 0L
    private var closed = false
    var safeArea = Rect()
        private set
    var night: Boolean = context.isDarkMode
        set(value) {
            field = value
            requestFrame(force = true)
        }

    private val frame = Runnable {
        val surface: Surface
        val data: DriveTelemetry
        val car: VehicleData
        synchronized(lock) {
            surface = target?.takeIf { it.isValid } ?: return@Runnable
            data = telemetry ?: return@Runnable
            car = vehicle
        }
        var canvas: android.graphics.Canvas? = null
        try {
            canvas = surface.lockCanvas(null)
            renderer.draw(canvas, data, car, night)
            lastFrameAt = SystemClock.elapsedRealtime()
        } catch (_: RuntimeException) {
            // The host may replace its surface while a coalesced frame is pending.
        } finally {
            if (canvas != null) runCatching { surface.unlockCanvasAndPost(canvas) }
        }
    }

    private val clockFrame = object : Runnable {
        override fun run() {
            requestFrame(force = true)
            handler.postDelayed(this, 60_000L - System.currentTimeMillis() % 60_000L)
        }
    }

    override fun onSurfaceAvailable(surface: SurfaceContainer) {
        synchronized(lock) {
            target = surface.surface
        }
        handler.removeCallbacks(clockFrame)
        handler.post(clockFrame)
        requestFrame(force = true)
    }

    fun update(value: DriveTelemetry) {
        synchronized(lock) { telemetry = value }
        renderer.record(value)
        requestFrame()
    }

    fun updateVehicle(value: VehicleData) {
        synchronized(lock) { vehicle = value }
        requestFrame()
    }

    private fun requestFrame(force: Boolean = false) {
        if (closed) return
        handler.removeCallbacks(frame)
        val elapsed = SystemClock.elapsedRealtime() - lastFrameAt
        val delay = if (force) 0L else (MIN_FRAME_INTERVAL_MS - elapsed).coerceAtLeast(0L)
        handler.postDelayed(frame, delay)
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) {
        safeArea = Rect(visibleArea)
        requestFrame(force = true)
    }

    override fun onStableAreaChanged(stableArea: Rect) {
        safeArea = Rect(stableArea)
        requestFrame(force = true)
    }

    override fun onSurfaceDestroyed(surface: SurfaceContainer) {
        handler.removeCallbacks(frame)
        handler.removeCallbacks(clockFrame)
        synchronized(lock) {
            target = null
        }
    }

    fun close() {
        closed = true
        handler.removeCallbacksAndMessages(null)
        synchronized(lock) {
            target = null
        }
        thread.quitSafely()
    }

    private companion object { const val MIN_FRAME_INTERVAL_MS = 1_000L }
}
