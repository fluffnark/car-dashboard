package com.fluffnark.motoringdashboard.car

import android.content.Intent
import android.content.res.Configuration
import com.fluffnark.motoringdashboard.map.MapScreen
import androidx.car.app.Screen
import androidx.car.app.Session

class DashboardSession : Session() {
    private var mapScreen: MapScreen? = null

    override fun onCreateScreen(intent: Intent): Screen = MapScreen(carContext).also { mapScreen = it }
    override fun onCarConfigurationChanged(newConfiguration: Configuration) {
        mapScreen?.surface?.night = carContext.isDarkMode
    }
}
