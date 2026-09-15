package com.fluffnark.motoringdashboard.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.SharedPreferences
import androidx.core.content.edit
import java.io.File

enum class DisplayTheme { NIGHT, AUTO, DAY }

object DisplayPreferences {
    private const val FILE = "display_preferences"
    private const val THEME = "theme"
    private const val BACKGROUND_FILE = "dashboard_background.jpg"
    private const val BACKGROUND_VERSION = "background_version"

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

    fun backgroundFile(context: Context): File? =
        File(context.applicationContext.filesDir, BACKGROUND_FILE).takeIf { it.isFile && it.length() > 0L }

    fun hasBackground(context: Context): Boolean = backgroundFile(context) != null

    /** Imports a single static image and normalizes it for low-memory car-surface rendering. */
    fun importBackground(context: Context, uri: Uri): Boolean = runCatching {
        val resolver = context.applicationContext.contentResolver
        val decoded = resolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
            ?: return false
        val maxDimension = maxOf(decoded.width, decoded.height)
        val scale = minOf(1f, 1600f / maxDimension.toFloat())
        val normalized = if (scale < 1f) Bitmap.createScaledBitmap(
            decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true,
        ) else decoded
        File(context.applicationContext.filesDir, BACKGROUND_FILE).outputStream().use {
            check(normalized.compress(Bitmap.CompressFormat.JPEG, 78, it))
        }
        if (normalized !== decoded) normalized.recycle()
        if (!decoded.isRecycled) decoded.recycle()
        val nextVersion = preferences(context).getLong(BACKGROUND_VERSION, 0L) + 1L
        preferences(context).edit { putLong(BACKGROUND_VERSION, nextVersion) }
        true
    }.getOrDefault(false)

    fun clearBackground(context: Context) {
        backgroundFile(context)?.delete()
        val nextVersion = preferences(context).getLong(BACKGROUND_VERSION, 0L) + 1L
        preferences(context).edit { putLong(BACKGROUND_VERSION, nextVersion) }
    }
}
