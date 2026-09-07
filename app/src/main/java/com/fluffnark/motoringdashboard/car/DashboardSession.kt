package com.fluffnark.motoringdashboard.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.fluffnark.motoringdashboard.data.DashboardRepository
import com.fluffnark.motoringdashboard.data.DashboardState

class DashboardSession : Session() {
    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                DashboardRepository.reset(connected = true)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                // Never carry readings from a previous vehicle connection into the next one.
                DashboardRepository.reset()
            }
        })
    }

    override fun onCreateScreen(intent: Intent): Screen = DashboardScreen(carContext)
}
