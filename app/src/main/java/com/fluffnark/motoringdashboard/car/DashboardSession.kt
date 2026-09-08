package com.fluffnark.motoringdashboard.car

import android.content.Intent
import android.content.res.Configuration
import com.fluffnark.motoringdashboard.map.MapScreen
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.fluffnark.motoringdashboard.data.DashboardRepository

class DashboardSession : Session() {
    private var mapScreen: MapScreen? = null
    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) = DashboardRepository.reset(connected = true)
            override fun onDestroy(owner: LifecycleOwner) = DashboardRepository.reset()
        })
    }

    override fun onCreateScreen(intent: Intent): Screen = MapScreen(carContext).also { mapScreen = it }
    override fun onCarConfigurationChanged(newConfiguration: Configuration) {
        mapScreen?.surface?.let { it.night = carContext.isDarkMode; it.applyStyle() }
    }
}
