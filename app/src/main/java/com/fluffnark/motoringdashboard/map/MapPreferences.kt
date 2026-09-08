package com.fluffnark.motoringdashboard.map

import android.content.Context

object MapPreferences {
    private const val FILE = "motoring_map"
    private const val LOOK = "look"

    fun getLook(context: Context): MapStyle.Look {
        val saved = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(LOOK, null)
        return MapStyle.Look.entries.firstOrNull { it.name == saved } ?: MapStyle.Look.WARM
    }

    fun setLook(context: Context, look: MapStyle.Look) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(LOOK, look.name).apply()
    }
}
