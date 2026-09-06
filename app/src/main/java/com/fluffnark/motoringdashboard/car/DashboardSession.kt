package com.fluffnark.motoringdashboard.car

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session

class DashboardSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = DashboardScreen(carContext)
}
