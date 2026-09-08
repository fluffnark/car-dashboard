package com.fluffnark.motoringdashboard.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import com.fluffnark.motoringdashboard.data.DriveTelemetry

object TelemetryArtwork {
    fun elevation(samples: List<DriveTelemetry>, night: Boolean): CarIcon {
        val bitmap = Bitmap.createBitmap(192, 64, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 3f; style = Paint.Style.STROKE; color = if (night) Color.rgb(199,149,96) else Color.rgb(173,116,69) }
        val elevations = samples.mapNotNull { it.elevationFeet }
        if (elevations.size > 1) {
            val low = elevations.min()
            val range = (elevations.max() - low).coerceAtLeast(100f)
            elevations.zipWithNext().forEachIndexed { index, (a, b) ->
                fun y(value: Float) = 58f - (value - low) / range * 50f
                canvas.drawLine(index * 191f / (elevations.size - 1), y(a),
                    (index + 1) * 191f / (elevations.size - 1), y(b), paint)
            }
        }
        return CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build()
    }
}
