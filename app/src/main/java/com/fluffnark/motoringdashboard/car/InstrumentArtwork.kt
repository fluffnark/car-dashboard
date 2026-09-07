package com.fluffnark.motoringdashboard.car

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.LruCache
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import com.fluffnark.motoringdashboard.data.SpeedSample

/** Small, opaque instrument images for the host's grid, not a projected phone screen. */
internal class InstrumentArtwork {
    private val cache = LruCache<String, CarIcon>(12)

    fun dial(value: String, unit: String, fraction: Float?, history: List<SpeedSample> = emptyList()): CarIcon {
        // Cache the displayed precision, so high-frequency sensor events do not allocate bitmaps.
        val step = fraction?.takeIf { it.isFinite() }?.coerceIn(0f, 1f)?.let { (it * 100).toInt() }
        val key = "$value|$unit|$step|$history"
        cache.get(key)?.let { return it }
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.rgb(28, 30, 31))
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.rgb(64, 67, 69)
        canvas.drawLine(24f, 210f, 232f, 210f, paint)
        if (unit == "MPH" && history.size > 1) {
            val end = history.last().elapsedMillis
            val ceiling = maxOf(60f, history.mapNotNull { it.mph }.maxOrNull() ?: 60f)
            paint.color = Color.rgb(225, 163, 65)
            paint.strokeWidth = 3f
            history.zipWithNext().forEach { (a, b) ->
                val first = a.mph
                val second = b.mph
                if (first != null && second != null && b.elapsedMillis - a.elapsedMillis <= 5_000) {
                    fun x(sample: SpeedSample) = 24f + ((sample.elapsedMillis - end + 120_000) / 120_000f).coerceIn(0f, 1f) * 208f
                    canvas.drawLine(x(a), 210f - first / ceiling * 42f,
                        x(b), 210f - second / ceiling * 42f, paint)
                }
            }
        } else if (step != null) {
            paint.color = Color.rgb(225, 163, 65)
            paint.strokeWidth = 5f
            canvas.drawLine(24f, 210f, 24f + 208f * step / 100f, 210f, paint)
        }
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(241, 242, 238)
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        paint.textSize = if (value.length > 4) 60f else 78f
        canvas.drawText(value, 128f, 116f, paint)
        paint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        paint.textSize = 17f
        paint.color = Color.rgb(163, 168, 170)
        canvas.drawText(unit, 128f, 150f, paint)
        paint.textSize = 13f
        canvas.drawText(if (unit == "MPH") "2 MIN · SPEED" else if (unit == "FUEL") "TANK LEVEL" else "LOCAL TIME", 128f, 239f, paint)
        val icon = CarIcon.Builder(IconCompat.createWithBitmap(bitmap))
            .build()
        cache.put(key, icon)
        return icon
    }
}
