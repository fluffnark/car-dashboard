package com.fluffnark.motoringdashboard.map

import android.app.Presentation
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.content.pm.ApplicationInfo
import android.widget.FrameLayout
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import com.fluffnark.motoringdashboard.data.DriveTelemetry
import com.fluffnark.motoringdashboard.debug.SimulationRoute
import com.fluffnark.motoringdashboard.poi.PoiRepository
import org.maplibre.geojson.FeatureCollection

/** GPU composition into the host Surface; no screenshot loop or full-frame bitmap copies. */
class CarMapSurface(private val context: CarContext) : SurfaceCallback {
    private val showDemoRoute = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    private var display: VirtualDisplay? = null
    private var presentation: Presentation? = null
    private var view: MapView? = null
    var map: MapLibreMap? = null
        private set
    var night = context.isDarkMode
    var look = MapPreferences.getLook(context)
    var center = LatLng(38.0228, -107.6714)
    var following = true
    var safeArea = Rect()
        private set
    var onReady: (() -> Unit)? = null

    override fun onSurfaceAvailable(surface: SurfaceContainer) {
        close()
        if (surface.width <= 0 || surface.height <= 0 || surface.surface == null) return
        MapLibre.getInstance(context.applicationContext)
        display = context.getSystemService(DisplayManager::class.java).createVirtualDisplay(
            "MotoringMap", surface.width, surface.height, surface.dpi.coerceAtLeast(1), surface.surface, 0)
        val window = Presentation(context, display!!.display)
        presentation = window
        val mapView = MapView(window.context, MapLibreMapOptions.createFromAttributes(window.context).textureMode(true))
        view = mapView
        window.setContentView(mapView, FrameLayout.LayoutParams(-1, -1))
        mapView.onCreate(null)
        window.show()
        mapView.onStart()
        mapView.onResume()
        mapView.getMapAsync { ready ->
            map = ready
            ready.uiSettings.apply { isCompassEnabled = false; isLogoEnabled = false; isAttributionEnabled = true }
            ready.cameraPosition = CameraPosition.Builder().target(center).zoom(13.0).build()
            applyStyle()
        }
    }

    fun applyStyle() {
        map?.setStyle(Style.Builder().fromJson(MapStyle.json(night, look))) { style ->
            if (showDemoRoute) {
                val route = SimulationRoute.coordinates().map { Point.fromLngLat(it.first, it.second) }
                style.addSource(GeoJsonSource("demo-route", Feature.fromGeometry(LineString.fromLngLats(route))))
                style.addLayer(LineLayer("demo-route-line", "demo-route").withProperties(
                    lineColor(if (night) "#c79560" else "#ad7445"), lineWidth(4f), lineOpacity(0.8f)))
            }
            val places = PoiRepository.demo.map { poi ->
                Feature.fromGeometry(Point.fromLngLat(poi.longitude, poi.latitude))
            }
            style.addSource(GeoJsonSource("places", FeatureCollection.fromFeatures(places)))
            style.addLayer(CircleLayer("place-dots", "places").withProperties(
                circleRadius(5f), circleColor(if (night) "#c79560" else "#ad7445"),
                circleStrokeColor(if (night) "#242521" else "#f4efe5"), circleStrokeWidth(2f)))
            style.addSource(GeoJsonSource("vehicle", Feature.fromGeometry(Point.fromLngLat(center.longitude, center.latitude))))
            style.addLayer(CircleLayer("vehicle-dot", "vehicle").withProperties(
                circleRadius(8f), circleColor(if (night) "#eee7d8" else "#34372f"),
                circleStrokeColor(if (night) "#242521" else "#f4efe5"), circleStrokeWidth(3f)))
            onReady?.invoke()
        }
    }

    fun update(telemetry: DriveTelemetry) {
        center = LatLng(telemetry.latitude, telemetry.longitude)
        map?.style?.getSourceAs<GeoJsonSource>("vehicle")
            ?.setGeoJson(Feature.fromGeometry(Point.fromLngLat(telemetry.longitude, telemetry.latitude)))
        if (following) map?.moveCamera(CameraUpdateFactory.newCameraPosition(
            CameraPosition.Builder().target(center).zoom(13.8).bearing(telemetry.headingDegrees.toDouble()).tilt(25.0).build()))
    }

    fun recenter() {
        following = true
        map?.moveCamera(CameraUpdateFactory.newLatLng(center))
    }

    fun zoom(delta: Double) { map?.let { it.moveCamera(CameraUpdateFactory.zoomTo((it.cameraPosition.zoom + delta).coerceIn(3.0, 18.0))) } }
    override fun onScroll(distanceX: Float, distanceY: Float) {
        following = false
        map?.let { m ->
            val point = m.projection.toScreenLocation(m.cameraPosition.target!!)
            point.offset(distanceX, distanceY)
            m.moveCamera(CameraUpdateFactory.newLatLng(m.projection.fromScreenLocation(point)))
        }
    }
    override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
        if (scaleFactor.isFinite() && scaleFactor > 0) zoom(kotlin.math.ln(scaleFactor.toDouble()) / kotlin.math.ln(2.0))
    }
    override fun onVisibleAreaChanged(visibleArea: Rect) {
        safeArea = Rect(visibleArea)
        val v = view ?: return
        map?.setPadding(visibleArea.left, visibleArea.top, (v.width - visibleArea.right).coerceAtLeast(0), (v.height - visibleArea.bottom).coerceAtLeast(0))
    }
    override fun onStableAreaChanged(stableArea: Rect) { safeArea = Rect(stableArea) }
    override fun onSurfaceDestroyed(surface: SurfaceContainer) = close()
    fun close() {
        view?.onPause()
        view?.onStop()
        view?.onDestroy()
        view = null
        map = null
        presentation?.dismiss()
        presentation = null
        display?.release()
        display = null
    }
}
