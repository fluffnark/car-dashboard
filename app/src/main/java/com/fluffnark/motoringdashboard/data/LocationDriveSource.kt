package com.fluffnark.motoringdashboard.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager

class LocationDriveSource(context: Context, private val onTelemetry: (DriveTelemetry) -> Unit) : LocationListener {
    private val manager = context.getSystemService(LocationManager::class.java)
    private var previous: Location? = null
    private var tripMeters = 0f

    @SuppressLint("MissingPermission")
    fun start() {
        val provider = when {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return
        }
        manager.requestLocationUpdates(provider, 1_000L, 2f, this)
        manager.getLastKnownLocation(provider)?.let(::onLocationChanged)
    }

    fun stop() = manager.removeUpdates(this)

    override fun onLocationChanged(location: Location) {
        val old = previous
        val segment = old?.distanceTo(location)?.takeIf { it in 0f..1_000f } ?: 0f
        tripMeters += segment
        val heading = when {
            location.hasBearing() -> location.bearing
            old != null && segment > 3f -> old.bearingTo(location)
            else -> 0f
        }
        val elevation = location.takeIf(Location::hasAltitude)?.altitude?.times(3.28084)?.toFloat()
        val previousElevation = old?.takeIf(Location::hasAltitude)?.altitude?.times(3.28084)?.toFloat()
        val grade = if (segment > 10f && elevation != null && previousElevation != null) {
            (elevation - previousElevation) / (segment * 3.28084f) * 100f
        } else null
        onTelemetry(DriveTelemetry(location.latitude, location.longitude,
            if (location.hasSpeed()) location.speed * 2.2369363f else 0f,
            heading, elevation, tripMeters * 0.0006213712f, grade,
            "Current road unavailable", if (location.hasAccuracy()) location.accuracy * 3.28084f else null))
        previous = location
    }
}
