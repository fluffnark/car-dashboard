package com.fluffnark.motoringdashboard.car

import android.os.Handler
import android.os.Looper
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.common.CarValue
import androidx.car.app.hardware.common.OnCarDataAvailableListener
import androidx.car.app.hardware.info.EnergyLevel
import androidx.car.app.hardware.info.Mileage
import androidx.car.app.hardware.info.Model
import androidx.car.app.hardware.info.Speed
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.fluffnark.motoringdashboard.data.DashboardFormat
import com.fluffnark.motoringdashboard.data.DashboardRepository
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val CAR_SPEED = "com.google.android.gms.permission.CAR_SPEED"
private const val CAR_FUEL = "com.google.android.gms.permission.CAR_FUEL"
private const val CAR_MILEAGE = "com.google.android.gms.permission.CAR_MILEAGE"

class DashboardScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {
    private val carInfo = carContext.getCarService(CarHardwareManager::class.java).carInfo
    private val handler = Handler(Looper.getMainLooper())
    private var listenersRegistered = false

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
        registerListeners()
    }

    override fun onStop(owner: LifecycleOwner) {
        DashboardRepository.setConnected(false)
        handler.removeCallbacks(minuteTicker)
        unregisterListeners()
    }

    private fun registerListeners() {
        if (listenersRegistered) return
        listenersRegistered = true
        val executor = carContext.mainExecutor
        carInfo.fetchModel(executor, modelListener)
        carInfo.addSpeedListener(executor, speedListener)
        carInfo.addEnergyLevelListener(executor, energyListener)
        carInfo.addMileageListener(executor, mileageListener)
    }

    private fun unregisterListeners() {
        if (!listenersRegistered) return
        listenersRegistered = false
        carInfo.removeSpeedListener(speedListener)
        carInfo.removeEnergyLevelListener(energyListener)
        carInfo.removeMileageListener(mileageListener)
    }

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

        @Suppress("DEPRECATION")
        return PaneTemplate.Builder(pane)
            .setTitle("Motoring Dashboard")
            .setHeaderAction(Action.APP_ICON)
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
