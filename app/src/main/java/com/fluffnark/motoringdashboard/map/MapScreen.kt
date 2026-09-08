package com.fluffnark.motoringdashboard.map

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.car.app.navigation.model.MapController
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.fluffnark.motoringdashboard.ChatGptLauncher
import com.fluffnark.motoringdashboard.R
import com.fluffnark.motoringdashboard.data.DriveTelemetry
import com.fluffnark.motoringdashboard.data.LocationDriveSource
import com.fluffnark.motoringdashboard.data.TelemetryFormat
import com.fluffnark.motoringdashboard.data.VehicleData
import com.fluffnark.motoringdashboard.data.VehicleDataSource
import com.fluffnark.motoringdashboard.data.DashboardFormat
import com.fluffnark.motoringdashboard.debug.SimulationRoute
import com.fluffnark.motoringdashboard.poi.PoiRepository
import androidx.core.graphics.drawable.IconCompat

class MapScreen(context: CarContext) : Screen(context), DefaultLifecycleObserver {
    val surface = CarMapSurface(context)
    private val handler = Handler(Looper.getMainLooper())
    private val useDemo = carContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    private var telemetry = if (useDemo) SimulationRoute.sample(0) else DriveTelemetry(
        latitude = 38.0228,
        longitude = -107.6714,
        speedMph = 0f,
        headingDegrees = 0f,
        elevationFeet = null,
        tripMiles = 0f,
        gradePercent = null,
        road = "Waiting for location",
    )
    private var hasPosition = useDemo
    private var step = 0
    private var showPlaces = false
    private var vehicle = VehicleData()
    private val history = ArrayDeque<DriveTelemetry>()
    private val location = LocationDriveSource(context, ::acceptTelemetry)
    private val vehicleSource = VehicleDataSource(context) { value ->
        vehicle = value
        invalidate()
    }
    private val simulation = object : Runnable {
        override fun run() {
            acceptTelemetry(SimulationRoute.sample(step++))
            handler.postDelayed(this, 3_000)
        }
    }
    init { lifecycle.addObserver(this) }
    override fun onStart(owner: LifecycleOwner) {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(surface)
        surface.onReady = { surface.update(telemetry) }
        vehicleSource.start()
        if (useDemo) handler.post(simulation) else if (hasLocation()) location.start()
    }
    override fun onStop(owner: LifecycleOwner) { handler.removeCallbacks(simulation); location.stop(); vehicleSource.stop() }
    override fun onDestroy(owner: LifecycleOwner) { handler.removeCallbacks(simulation); location.stop(); vehicleSource.stop(); surface.close() }

    override fun onGetTemplate(): Template {
        return MapWithContentTemplate.Builder().setContentTemplate(if (showPlaces) places() else instruments())
            .setActionStrip(ActionStrip.Builder()
                .addAction(iconAction(R.drawable.ic_places, "Places") { showPlaces = !showPlaces; invalidate() })
                .addAction(Action.Builder().setTitle("Voice").setOnClickListener(ParkedOnlyOnClickListener.create { ChatGptLauncher.open(carContext) }).build())
                .addAction(iconAction(R.drawable.ic_layers, "Map style") {
                    surface.look = MapStyle.Look.entries[(surface.look.ordinal + 1) % 3]
                    MapPreferences.setLook(carContext, surface.look)
                    surface.applyStyle()
                })
                .build())
            .setMapController(MapController.Builder().setMapActionStrip(ActionStrip.Builder()
                .addAction(Action.PAN)
                .addAction(iconAction(R.drawable.ic_zoom_in, "Zoom in") { surface.zoom(1.0) })
                .addAction(iconAction(R.drawable.ic_zoom_out, "Zoom out") { surface.zoom(-1.0) })
                .addAction(iconAction(R.drawable.ic_center, "Recenter") { surface.recenter() })
                .build()).build())
            .build()
    }

    private fun instruments(): PaneTemplate = PaneTemplate.Builder(Pane.Builder()
        .addRow(Row.Builder().setTitle("${(vehicle.speedMph ?: telemetry.speedMph).toInt()} mph · ${TelemetryFormat.heading(telemetry.headingDegrees)}")
            .addText(telemetry.road).build())
        .addRow(Row.Builder().setTitle("${TelemetryFormat.feet(telemetry.elevationFeet)} · ${TelemetryFormat.grade(telemetry.gradePercent)} grade")
            .addText("Trip ${"%.1f".format(telemetry.tripMiles)} mi${if (telemetry.demo) " · DEMO" else ""}").build())
        .addRow(Row.Builder().setTitle("Elevation profile")
            .addText(vehicle.fuelPercent?.let { "Fuel ${DashboardFormat.percent(it)} · range ${DashboardFormat.miles(vehicle.rangeMiles)}" }
                ?: "Recent climb · ${history.size} samples")
            .setImage(TelemetryArtwork.elevation(history.toList(), carContext.isDarkMode)).build())
        .apply {
            if (!useDemo && !hasLocation()) addAction(Action.Builder().setTitle("Allow location")
                .setOnClickListener { carContext.requestPermissions(listOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)) { _, _ -> if (hasLocation()) location.start(); invalidate() } }.build())
            if (!vehicleSource.hasAllPermissions()) addAction(Action.Builder().setTitle("Allow car data")
                .setOnClickListener { vehicleSource.requestPermissions(::invalidate) }.build())
        }.build()).setHeader(Header.Builder().setTitle("Motoring").build()).build()

    private fun places(): ListTemplate {
        val list = ItemList.Builder()
        val nearby = if (hasPosition) PoiRepository.nearby(telemetry.latitude, telemetry.longitude) else emptyList()
        nearby.forEach { (poi, miles) ->
            list.addItem(Row.Builder().setTitle(poi.name).addText("${poi.detail} · ${"%.1f".format(miles)} mi")
                .setBrowsable(false).setOnClickListener {
                    carContext.startCarApp(Intent(CarContext.ACTION_NAVIGATE,
                        Uri.parse("geo:${poi.latitude},${poi.longitude}?q=${poi.latitude},${poi.longitude}(${Uri.encode(poi.name)})")))
                }.build())
        }
        if (nearby.isEmpty()) {
            list.addItem(Row.Builder().setTitle("No guide places nearby")
                .addText("The first curated guide covers the San Juan Mountains. Map tracking still works everywhere.")
                .build())
        }
        return ListTemplate.Builder().setSingleList(list.build())
            .setHeader(Header.Builder().setTitle("Nearby places").build()).build()
    }

    private fun iconAction(resource: Int, description: String, click: () -> Unit): Action = Action.Builder()
        .setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, resource)).build())
        .setOnClickListener(click).build()

    private fun hasLocation(): Boolean = carContext.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        carContext.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun acceptTelemetry(value: DriveTelemetry) {
        telemetry = value
        hasPosition = true
        history.addLast(value)
        while (history.size > 60) history.removeFirst()
        surface.update(value)
        invalidate()
    }
}
