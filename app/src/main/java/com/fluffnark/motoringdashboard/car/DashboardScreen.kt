package com.fluffnark.motoringdashboard.car

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaBrowserCompat
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.common.CarValue
import androidx.car.app.hardware.common.OnCarDataAvailableListener
import androidx.car.app.hardware.info.EnergyLevel
import androidx.car.app.hardware.info.Mileage
import androidx.car.app.hardware.info.Model
import androidx.car.app.hardware.info.Speed
import androidx.car.app.media.MediaPlaybackManager
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.fluffnark.motoringdashboard.data.DashboardFormat
import com.fluffnark.motoringdashboard.data.DashboardRepository
import com.fluffnark.motoringdashboard.media.ConceptMediaService
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val CAR_SPEED = "com.google.android.gms.permission.CAR_SPEED"
private const val CAR_FUEL = "com.google.android.gms.permission.CAR_FUEL"
private const val CAR_MILEAGE = "com.google.android.gms.permission.CAR_MILEAGE"

class DashboardScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {
    private val carInfo = carContext.getCarService(CarHardwareManager::class.java).carInfo
    private val handler = Handler(Looper.getMainLooper())
    private var speedRegistered = false
    private var energyRegistered = false
    private var mileageRegistered = false
    private var mediaBrowser: MediaBrowserCompat? = null
    private var mediaPlaybackReady = false

    private val mediaConnection = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            val browser = mediaBrowser ?: return
            mediaPlaybackReady = runCatching {
                carContext.getCarService(MediaPlaybackManager::class.java)
                    .registerMediaPlaybackToken(browser.sessionToken)
            }.isSuccess
            invalidate()
        }

        override fun onConnectionSuspended() {
            mediaPlaybackReady = false
            invalidate()
        }

        override fun onConnectionFailed() {
            mediaPlaybackReady = false
            invalidate()
        }
    }

    private val minuteTicker = object : Runnable {
        override fun run() {
            invalidate()
            handler.postDelayed(this, 60_000L)
        }
    }

    private val speedListener = OnCarDataAvailableListener<Speed> { reading ->
        val display = reading.displaySpeedMetersPerSecond.successfulValue()
        val raw = reading.rawSpeedMetersPerSecond.successfulValue()
        DashboardRepository.update {
            it.copy(
                speedMph = display?.let(DashboardFormat::metersPerSecondToMph),
                rawSpeedMph = raw?.let(DashboardFormat::metersPerSecondToMph),
            )
        }
        invalidate()
    }
    private val energyListener = OnCarDataAvailableListener<EnergyLevel> { reading ->
        DashboardRepository.update {
            it.copy(
                fuelPercent = reading.fuelPercent.successfulValue(),
                rangeMiles = reading.rangeRemainingMeters.successfulValue()?.let(DashboardFormat::metersToMiles),
            )
        }
        invalidate()
    }
    private val mileageListener = OnCarDataAvailableListener<Mileage> { reading ->
        DashboardRepository.update {
            it.copy(odometerMiles = reading.odometerInKilometers.successfulValue()?.let(DashboardFormat::kilometersToMiles))
        }
        invalidate()
    }
    private val modelListener = OnCarDataAvailableListener<Model> { model ->
        val maker = model.manufacturer.successfulValue()
        val name = model.name.successfulValue()
        val year = model.year.successfulValue()?.toString()
        val label = listOfNotNull(year, maker, name).joinToString(" ").ifBlank { null }
        DashboardRepository.update { it.copy(vehicleName = label) }
        invalidate()
    }

    init {
        lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        DashboardRepository.setConnected(true)
        handler.post(minuteTicker)
        connectMediaSession()
        registerListeners()
    }

    override fun onStop(owner: LifecycleOwner) {
        DashboardRepository.setConnected(false)
        handler.removeCallbacks(minuteTicker)
        mediaBrowser?.disconnect()
        mediaBrowser = null
        mediaPlaybackReady = false
        unregisterListeners()
    }

    private fun connectMediaSession() {
        if (mediaBrowser != null) return
        mediaBrowser = MediaBrowserCompat(
            carContext,
            ComponentName(carContext, ConceptMediaService::class.java),
            mediaConnection,
            null,
        ).also(MediaBrowserCompat::connect)
    }

    private fun registerListeners() {
        val executor = carContext.mainExecutor
        runCatching { carInfo.fetchModel(executor, modelListener) }

        if (hasPermission(CAR_SPEED) && !speedRegistered) {
            speedRegistered = runCatching {
                carInfo.addSpeedListener(executor, speedListener)
            }.isSuccess
        }
        if (hasPermission(CAR_FUEL) && !energyRegistered) {
            energyRegistered = runCatching {
                carInfo.addEnergyLevelListener(executor, energyListener)
            }.isSuccess
        }
        if (hasPermission(CAR_MILEAGE) && !mileageRegistered) {
            mileageRegistered = runCatching {
                carInfo.addMileageListener(executor, mileageListener)
            }.isSuccess
        }
    }

    private fun unregisterListeners() {
        if (speedRegistered) runCatching { carInfo.removeSpeedListener(speedListener) }
        if (energyRegistered) runCatching { carInfo.removeEnergyLevelListener(energyListener) }
        if (mileageRegistered) runCatching { carInfo.removeMileageListener(mileageListener) }
        speedRegistered = false
        energyRegistered = false
        mileageRegistered = false
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(carContext, permission) == PackageManager.PERMISSION_GRANTED

    override fun onGetTemplate(): Template {
        val state = DashboardRepository.state.value
        val now = ZonedDateTime.now()
        val time = now.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
        val date = now.format(DateTimeFormatter.ofPattern("EEEE · MMMM d", Locale.US))

        val pane = Pane.Builder()
            .addRow(Row.Builder().setTitle(time).addText(date).build())
            .addRow(
                Row.Builder()
                    .setTitle("${DashboardFormat.speed(state.speedMph)} mph")
                    .addText(state.rawSpeedMph?.let { "Raw speed ${DashboardFormat.speed(it)} mph" } ?: "Vehicle speed unavailable")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Fuel ${DashboardFormat.percent(state.fuelPercent)}")
                    .addText("Range ${DashboardFormat.miles(state.rangeMiles)}")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle(state.vehicleName ?: "Vehicle")
                    .addText("Odometer ${DashboardFormat.miles(state.odometerMiles)}")
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Allow vehicle data")
                    .setOnClickListener(::requestVehiclePermissions)
                    .build()
            )
            .build()

        val header = Header.Builder()
            .setTitle("Motoring Dashboard")
            .setStartHeaderAction(Action.APP_ICON)
            .apply {
                // Hosts reject this action until a media token has been registered.
                if (mediaPlaybackReady) addEndHeaderAction(Action.MEDIA_PLAYBACK)
            }
            .build()

        return PaneTemplate.Builder(pane)
            .setHeader(header)
            .build()
    }

    private fun requestVehiclePermissions() {
        carContext.requestPermissions(listOf(CAR_SPEED, CAR_FUEL, CAR_MILEAGE)) { _, _ ->
            unregisterListeners()
            registerListeners()
            invalidate()
        }
    }
}

private fun <T> CarValue<T>.successfulValue(): T? =
    if (status == CarValue.STATUS_SUCCESS) value else null
