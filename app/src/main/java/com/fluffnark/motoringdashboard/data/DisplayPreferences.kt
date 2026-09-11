package com.fluffnark.motoringdashboard.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

enum class DisplayTheme { NIGHT, AUTO, DAY }

object DisplayPreferences {
    private const val FILE = "display_preferences"
    private const val THEME = "theme"

    fun preferences(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun theme(context: Context): DisplayTheme {
        val saved = preferences(context).getString(THEME, null)
        return DisplayTheme.entries.firstOrNull { it.name == saved } ?: DisplayTheme.NIGHT
    }

    fun setTheme(context: Context, theme: DisplayTheme) {
        preferences(context).edit { putString(THEME, theme.name) }
    }

    fun isNight(context: Context, hostNight: Boolean): Boolean = when (theme(context)) {
        DisplayTheme.NIGHT -> true
        DisplayTheme.AUTO -> hostNight
        DisplayTheme.DAY -> false
    }
}
