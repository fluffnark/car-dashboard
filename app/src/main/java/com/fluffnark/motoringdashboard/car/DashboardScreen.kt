package com.fluffnark.motoringdashboard.car

import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.common.CarValue
import androidx.car.app.hardware.common.OnCarDataAvailableListener
import androidx.car.app.hardware.info.CarInfo
import androidx.car.app.hardware.info.EnergyLevel
import androidx.car.app.hardware.info.Mileage
import androidx.car.app.hardware.info.Model
import androidx.car.app.hardware.info.Speed
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
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val CAR_SPEED = "com.google.android.gms.permission.CAR_SPEED"
private const val CAR_FUEL = "com.google.android.gms.permission.CAR_FUEL"
private const val CAR_MILEAGE = "com.google.android.gms.permission.CAR_MILEAGE"

class DashboardScreen(carContext: CarContext) : Screen(carContext), DefaultLifecycleObserver {
    private val carInfo: CarInfo? by lazy {
        runCatching {
            carContext.getCarService(CarHardwareManager::class.java).carInfo
        }.getOrNull()
    }
    private val handler = Handler(Looper.getMainLooper())
    private var speedRegistered = false
    private var energyRegistered = false
    private var mileageRegistered = false

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
        val info = carInfo ?: return
        val executor = carContext.mainExecutor
        runCatching { info.fetchModel(executor, modelListener) }

        if (hasPermission(CAR_SPEED) && !speedRegistered) {
            speedRegistered = runCatching {
                info.addSpeedListener(executor, speedListener)
            }.isSuccess
        }
        if (hasPermission(CAR_FUEL) && !energyRegistered) {
            energyRegistered = runCatching {
                info.addEnergyLevelListener(executor, energyListener)
            }.isSuccess
        }
        if (hasPermission(CAR_MILEAGE) && !mileageRegistered) {
            mileageRegistered = runCatching {
                info.addMileageListener(executor, mileageListener)
            }.isSuccess
        }
    }

    private fun unregisterListeners() {
        val info = carInfo
        if (speedRegistered) runCatching { info?.removeSpeedListener(speedListener) }
        if (energyRegistered) runCatching { info?.removeEnergyLevelListener(energyListener) }
        if (mileageRegistered) runCatching { info?.removeMileageListener(mileageListener) }
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

        val paneBuilder = Pane.Builder()
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

        if (!hasAllVehiclePermissions()) {
            paneBuilder.addAction(
                Action.Builder()
                    .setTitle("Allow vehicle data")
                    .setOnClickListener(::requestVehiclePermissions)
                    .build()
            )
        }

        val header = Header.Builder()
            .setTitle("$time  ·  $date")
            .build()

        return PaneTemplate.Builder(paneBuilder.build())
            .setHeader(header)
            .build()
    }

    private fun hasAllVehiclePermissions(): Boolean =
        hasPermission(CAR_SPEED) && hasPermission(CAR_FUEL) && hasPermission(CAR_MILEAGE)

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
