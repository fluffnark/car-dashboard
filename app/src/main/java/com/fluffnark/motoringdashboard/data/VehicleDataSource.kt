package com.fluffnark.motoringdashboard.data

import android.content.pm.PackageManager
import androidx.car.app.CarContext
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.common.CarValue
import androidx.car.app.hardware.common.OnCarDataAvailableListener
import androidx.car.app.hardware.info.CarInfo
import androidx.car.app.hardware.info.EnergyLevel
import androidx.car.app.hardware.info.Mileage
import androidx.car.app.hardware.info.Model
import androidx.car.app.hardware.info.Speed
import androidx.core.content.ContextCompat

const val CAR_SPEED_PERMISSION = "com.google.android.gms.permission.CAR_SPEED"
const val CAR_FUEL_PERMISSION = "com.google.android.gms.permission.CAR_FUEL"
const val CAR_MILEAGE_PERMISSION = "com.google.android.gms.permission.CAR_MILEAGE"

data class VehicleData(
    val speedMph: Float? = null,
    val fuelPercent: Float? = null,
    val rangeMiles: Float? = null,
    val odometerMiles: Float? = null,
    val model: String? = null,
)

/** Optional Android Auto car-hardware readings. Unsupported values remain null. */
class VehicleDataSource(
    private val context: CarContext,
    private val onData: (VehicleData) -> Unit,
) {
    private val info: CarInfo? by lazy {
        runCatching { context.getCarService(CarHardwareManager::class.java).carInfo }.getOrNull()
    }
    private var data = VehicleData()
    private var speedRegistered = false
    private var energyRegistered = false
    private var mileageRegistered = false

    private val speedListener = OnCarDataAvailableListener<Speed> { reading ->
        update { copy(speedMph = (reading.displaySpeedMetersPerSecond.valueOrNull()
            ?: reading.rawSpeedMetersPerSecond.valueOrNull())?.let(DashboardFormat::metersPerSecondToMph)) }
    }
    private val energyListener = OnCarDataAvailableListener<EnergyLevel> { reading ->
        update { copy(
            fuelPercent = reading.fuelPercent.valueOrNull(),
            rangeMiles = reading.rangeRemainingMeters.valueOrNull()?.let(DashboardFormat::metersToMiles),
        ) }
    }
    private val mileageListener = OnCarDataAvailableListener<Mileage> { reading ->
        update { copy(odometerMiles = reading.odometerInKilometers.valueOrNull()?.let(DashboardFormat::kilometersToMiles)) }
    }
    private val modelListener = OnCarDataAvailableListener<Model> { reading ->
        val label = listOfNotNull(
            reading.year.valueOrNull()?.toString(),
            reading.manufacturer.valueOrNull(),
            reading.name.valueOrNull(),
        ).joinToString(" ").ifBlank { null }
        update { copy(model = label) }
    }

    fun start() {
        val carInfo = info ?: return
        runCatching { carInfo.fetchModel(context.mainExecutor, modelListener) }
        if (has(CAR_SPEED_PERMISSION) && !speedRegistered) {
            speedRegistered = runCatching { carInfo.addSpeedListener(context.mainExecutor, speedListener) }.isSuccess
        }
        if (has(CAR_FUEL_PERMISSION) && !energyRegistered) {
            energyRegistered = runCatching { carInfo.addEnergyLevelListener(context.mainExecutor, energyListener) }.isSuccess
        }
        if (has(CAR_MILEAGE_PERMISSION) && !mileageRegistered) {
            mileageRegistered = runCatching { carInfo.addMileageListener(context.mainExecutor, mileageListener) }.isSuccess
        }
    }

    fun stop() {
        if (speedRegistered) runCatching { info?.removeSpeedListener(speedListener) }
        if (energyRegistered) runCatching { info?.removeEnergyLevelListener(energyListener) }
        if (mileageRegistered) runCatching { info?.removeMileageListener(mileageListener) }
        speedRegistered = false
        energyRegistered = false
        mileageRegistered = false
    }

    fun hasAllPermissions(): Boolean = PERMISSIONS.all(::has)

    fun requestPermissions(onComplete: () -> Unit) {
        context.requestPermissions(PERMISSIONS.filterNot(::has)) { _, _ ->
            stop()
            start()
            onComplete()
        }
    }

    private fun has(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun update(block: VehicleData.() -> VehicleData) {
        data = data.block()
        onData(data)
    }

    companion object {
        val PERMISSIONS = listOf(CAR_SPEED_PERMISSION, CAR_FUEL_PERMISSION, CAR_MILEAGE_PERMISSION)
    }
}

private fun <T> CarValue<T>.valueOrNull(): T? = if (status == CarValue.STATUS_SUCCESS) value else null
