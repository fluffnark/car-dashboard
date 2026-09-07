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
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridSection
import androidx.car.app.model.Header
import androidx.car.app.model.SectionedItemTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.fluffnark.motoringdashboard.data.DashboardFormat
import com.fluffnark.motoringdashboard.data.DashboardRepository
import com.fluffnark.motoringdashboard.ChatGptLauncher
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val CAR_SPEED = "com.google.android.gms.permission.CAR_SPEED"
private const val CAR_FUEL = "com.google.android.gms.permission.CAR_FUEL"
private const val CAR_MILEAGE = "com.google.android.gms.permission.CAR_MILEAGE"

class DashboardScreen(carContext: CarContext, private val showDetails: Boolean = false) : Screen(carContext), DefaultLifecycleObserver {
    private val artwork = InstrumentArtwork()
    private val carInfo: CarInfo? by lazy {
        runCatching {
            carContext.getCarService(CarHardwareManager::class.java).carInfo
        }.getOrNull()
    }
    private val handler = Handler(Looper.getMainLooper())
    private var speedRegistered = false
    private var energyRegistered = false
    private var mileageRegistered = false

    private var lastDisplayKey: String? = null
    private val displayTicker = object : Runnable {
        override fun run() {
            // Coalesce sensor bursts; only send a new template when visible values change.
            val state = DashboardRepository.state.value
            val key = listOf(
                System.currentTimeMillis() / 60_000L,
                DashboardFormat.speed(state.speedMph ?: state.rawSpeedMph),
                DashboardFormat.speed(state.rawSpeedMph), DashboardFormat.percent(state.fuelPercent),
                DashboardFormat.miles(state.rangeMiles), DashboardFormat.miles(state.odometerMiles),
                state.vehicleName, hasAllVehiclePermissions(),
                state.speedHistory.lastOrNull(),
            ).joinToString("|")
            if (key != lastDisplayKey) {
                lastDisplayKey = key
                invalidate()
            }
            handler.postDelayed(this, 1_000L)
        }
    }

    private val speedListener = OnCarDataAvailableListener<Speed> { reading ->
        val display = reading.displaySpeedMetersPerSecond.successfulValue()
        val raw = reading.rawSpeedMetersPerSecond.successfulValue()
        DashboardRepository.recordSpeed(display?.let(DashboardFormat::metersPerSecondToMph),
            raw?.let(DashboardFormat::metersPerSecondToMph), android.os.SystemClock.elapsedRealtime())
    }
    private val energyListener = OnCarDataAvailableListener<EnergyLevel> { reading ->
        DashboardRepository.update {
            it.copy(
                fuelPercent = reading.fuelPercent.successfulValue(),
                rangeMiles = reading.rangeRemainingMeters.successfulValue()?.let(DashboardFormat::metersToMiles),
            )
        }
    }
    private val mileageListener = OnCarDataAvailableListener<Mileage> { reading ->
        DashboardRepository.update {
            it.copy(odometerMiles = reading.odometerInKilometers.successfulValue()?.let(DashboardFormat::kilometersToMiles))
        }
    }
    private val modelListener = OnCarDataAvailableListener<Model> { model ->
        val maker = model.manufacturer.successfulValue()
        val name = model.name.successfulValue()
        val year = model.year.successfulValue()?.toString()
        val label = listOfNotNull(year, maker, name).joinToString(" ").ifBlank { null }
        DashboardRepository.update { it.copy(vehicleName = label) }
    }

    init {
        lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        handler.removeCallbacks(displayTicker)
        lastDisplayKey = null
        handler.post(displayTicker)
        registerListeners()
    }

    override fun onStop(owner: LifecycleOwner) {
        handler.removeCallbacks(displayTicker)
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
        return if (showDetails) detailsTemplate() else instrumentsTemplate()
    }

    private fun instrumentsTemplate(): Template {
        val state = DashboardRepository.state.value
        val now = ZonedDateTime.now()
        val time = now.format(DateTimeFormatter.ofPattern("h:mm", Locale.getDefault()))
        val period = now.format(DateTimeFormatter.ofPattern("a", Locale.getDefault()))
        val speed = state.speedMph ?: state.rawSpeedMph
        val speedText = DashboardFormat.speed(speed)
        val fuelText = DashboardFormat.percent(state.fuelPercent)
        fun tile(title: String, text: String, icon: androidx.car.app.model.CarIcon) =
            GridItem.Builder().setTitle(title).setText(text).setImage(icon)
                .setOnClickListener { screenManager.push(DashboardScreen(carContext, showDetails = true)) }
                .build()

        val items = GridSection.Builder().setItemSize(GridSection.ITEM_SIZE_EXTRA_LARGE)
            .addItem(tile("Clock", "$time $period",
                artwork.dial(time, period, null)))
            .addItem(tile("Speed", if (speed == null) "Unavailable" else "$speedText mph",
                artwork.dial(speedText, "MPH", speed?.div(120f), state.speedHistory)))
            .addItem(tile("Fuel", "$fuelText · ${DashboardFormat.miles(state.rangeMiles)}",
                artwork.dial(fuelText, "FUEL", state.fuelPercent?.div(100f))))
            .build()
        // Stable titles make changing readings a refresh, avoiding the host's task-step limit.
        return SectionedItemTemplate.Builder()
            .addSection(items)
            .setHeader(Header.Builder().setTitle("Instruments")
                .addEndHeaderAction(Action.Builder().setTitle("Start ChatGPT")
                    .setOnClickListener(ParkedOnlyOnClickListener.create { ChatGptLauncher.open(carContext) })
                    .build())
                .build())
            .build()
    }

    private fun detailsTemplate(): Template {
        val state = DashboardRepository.state.value

        val paneBuilder = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle("Speed")
                    .addText("${DashboardFormat.speed(state.speedMph ?: state.rawSpeedMph)} mph")
                    .addText(state.rawSpeedMph?.let { "Raw speed ${DashboardFormat.speed(it)} mph" } ?: "Raw speed unavailable")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Fuel and range")
                    .addText("${DashboardFormat.percent(state.fuelPercent)} · Range ${DashboardFormat.miles(state.rangeMiles)}")
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Vehicle")
                    .addText(state.vehicleName ?: "Model not supplied by car")
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
            .setTitle("Vehicle data")
            .setStartHeaderAction(Action.BACK)
            .build()

        return PaneTemplate.Builder(paneBuilder.build())
            .setHeader(header)
            .build()
    }

    private fun hasAllVehiclePermissions(): Boolean =
        hasPermission(CAR_SPEED) && hasPermission(CAR_FUEL) && hasPermission(CAR_MILEAGE)

    private fun requestVehiclePermissions() {
        carContext.requestPermissions(listOf(CAR_SPEED, CAR_FUEL, CAR_MILEAGE).filterNot(::hasPermission)) { _, _ ->
            unregisterListeners()
            registerListeners()
            invalidate()
        }
    }
}

private fun <T> CarValue<T>.successfulValue(): T? =
    if (status == CarValue.STATUS_SUCCESS) value else null
