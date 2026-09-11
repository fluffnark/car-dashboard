package com.fluffnark.motoringdashboard.map

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.fluffnark.motoringdashboard.ChatGptLauncher
import com.fluffnark.motoringdashboard.R
import com.fluffnark.motoringdashboard.data.DriveTelemetry
import com.fluffnark.motoringdashboard.data.DisplayPreferences
import com.fluffnark.motoringdashboard.data.LocationDriveSource
import com.fluffnark.motoringdashboard.data.VehicleData
import com.fluffnark.motoringdashboard.data.VehicleDataSource
import com.fluffnark.motoringdashboard.debug.SimulationRoute
import com.fluffnark.motoringdashboard.trip.TripListRepository
import com.fluffnark.motoringdashboard.trip.TripListStore
import androidx.core.graphics.drawable.IconCompat

class MapScreen(context: CarContext) : Screen(context), DefaultLifecycleObserver {
    val surface = DashboardSurface(context).apply {
        night = DisplayPreferences.isNight(context, context.isDarkMode)
    }
    private val displayPreferences = DisplayPreferences.preferences(context)
    private val displayPreferenceListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        surface.night = DisplayPreferences.isNight(carContext, carContext.isDarkMode)
    }
    private val tripPreferences = context.getSharedPreferences("trip_list", android.content.Context.MODE_PRIVATE)
    private val tripPreferenceListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        invalidate()
    }
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
    private var content = Content.DASHBOARD
    private var vehicle = VehicleData()
    private val tripList: TripListStore = TripListRepository(context)
    private val location = LocationDriveSource(context, ::acceptTelemetry)
    private val vehicleSource = VehicleDataSource(context) { value ->
        vehicle = value
        surface.updateVehicle(value)
    }
    private val simulation = object : Runnable {
        override fun run() {
            acceptTelemetry(SimulationRoute.sample(step++))
            handler.postDelayed(this, 1_000)
        }
    }
    init { lifecycle.addObserver(this) }
    override fun onStart(owner: LifecycleOwner) {
        displayPreferences.registerOnSharedPreferenceChangeListener(displayPreferenceListener)
        tripPreferences.registerOnSharedPreferenceChangeListener(tripPreferenceListener)
        surface.night = DisplayPreferences.isNight(carContext, carContext.isDarkMode)
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(surface)
        surface.update(telemetry)
        surface.updateVehicle(vehicle)
        vehicleSource.start()
        if (useDemo) {
            handler.removeCallbacks(simulation)
            handler.post(simulation)
        } else if (hasLocation()) location.start()
    }
    override fun onStop(owner: LifecycleOwner) {
        displayPreferences.unregisterOnSharedPreferenceChangeListener(displayPreferenceListener)
        tripPreferences.unregisterOnSharedPreferenceChangeListener(tripPreferenceListener)
        handler.removeCallbacks(simulation)
        location.stop()
        vehicleSource.stop()
    }
    override fun onDestroy(owner: LifecycleOwner) { handler.removeCallbacks(simulation); location.stop(); vehicleSource.stop(); surface.close() }

    override fun onGetTemplate(): Template {
        val hasTripItems = tripList.items().isNotEmpty()
        val panel = when {
            content == Content.TRIP_LIST && hasTripItems -> tripList()
            else -> instruments()
        }
        val actions = ActionStrip.Builder()
        if (hasTripItems) {
            actions.addAction(iconAction(R.drawable.ic_checklist) {
                content = if (content == Content.TRIP_LIST) Content.DASHBOARD else Content.TRIP_LIST
                invalidate()
            })
        }
        actions.addAction(Action.Builder()
            .setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_voice)).build())
            .setOnClickListener(ParkedOnlyOnClickListener.create {
                ChatGptLauncher.open(carContext, returnToCar = true)
            })
            .setFlags(Action.FLAG_IS_PERSISTENT)
            .build())
        return MapWithContentTemplate.Builder().setContentTemplate(panel)
            .setActionStrip(actions.build())
            .build()
    }

    private fun instruments(): PaneTemplate = PaneTemplate.Builder(Pane.Builder()
        .addRow(Row.Builder()
            // Android Auto requires non-empty pane content; a single quiet mark keeps the host
            // panel at its minimum size without duplicating the compass or road information.
            .setTitle("·")
            .build())
        .apply {
            if (!useDemo && !hasLocation()) addAction(Action.Builder().setTitle("Allow location")
                .setOnClickListener { carContext.requestPermissions(listOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)) { _, _ -> if (hasLocation()) location.start(); invalidate() } }.build())
            if (!vehicleSource.hasAnyPermission()) addAction(Action.Builder().setTitle("Allow car data")
                .setOnClickListener { vehicleSource.requestPermissions(::invalidate) }.build())
        }.build()).build()

    private fun tripList(): ListTemplate {
        val list = ItemList.Builder()
        val items = tripList.items()
        items.sortedBy { it.done }.forEach { item ->
            list.addItem(Row.Builder()
                .setTitle(item.title)
                .setImage(CarIcon.Builder(IconCompat.createWithResource(carContext,
                    if (item.done) R.drawable.ic_checked else R.drawable.ic_unchecked)).build(), Row.IMAGE_TYPE_ICON)
                .setOnClickListener { tripList.toggle(item.id); invalidate() }
                .build())
        }
        return ListTemplate.Builder().setSingleList(list.build()).build()
    }

    private fun iconAction(resource: Int, click: () -> Unit): Action = Action.Builder()
        .setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, resource)).build())
        .setOnClickListener(click).setFlags(Action.FLAG_IS_PERSISTENT).build()

    private fun hasLocation(): Boolean = carContext.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        carContext.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun acceptTelemetry(value: DriveTelemetry) {
        val firstPosition = !hasPosition
        telemetry = value
        hasPosition = true
        surface.update(value)
        if (firstPosition) invalidate()
    }

    private enum class Content { DASHBOARD, TRIP_LIST }

}
