package com.fluffnark.motoringdashboard.map

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.fluffnark.motoringdashboard.R
import com.fluffnark.motoringdashboard.data.DriveTelemetry
import com.fluffnark.motoringdashboard.data.TelemetryFormat

/** A single quiet instrument strip drawn over the map surface. */
class TelemetryOverlayView(context: Context) : View(context) {
    private val typeface = ResourcesCompat.getFont(context, R.font.instrument_sans)
    private val elevations = ArrayDeque<Float>()
    private var current: DriveTelemetry? = null
    var night: Boolean = false
        set(value) { field = value; invalidate() }

    init {
        isClickable = false
        isFocusable = false
    }

    fun update(value: DriveTelemetry) {
        if (value.demo && current?.let { value.tripMiles < it.tripMiles } == true) {
            elevations.clear()
        }
        current = value
        value.elevationFeet?.let(elevations::addLast)
        while (elevations.size > 60) elevations.removeFirst()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val value = current ?: return
        val left = 14f
        val right = width - 106f
        val bottom = height - 14f
        val top = bottom - 104f
        if (right - left < 320f) return

        canvas.drawRoundRect(left, top, right, bottom, 10f, 10f, paint(
            if (night) Color.rgb(29, 31, 29) else Color.rgb(39, 41, 38),
            alpha = 224,
        ))
        val chalk = Color.rgb(237, 232, 220)
        val muted = Color.rgb(174, 177, 169)
        val clay = if (night) Color.rgb(205, 157, 104) else Color.rgb(190, 126, 76)

        text(canvas, value.elevationFeet?.let { "%,.0f".format(it) } ?: "—", left + 18f, top + 64f, 45f, chalk)
        text(canvas, "FT", left + 143f, top + 58f, 16f, muted)
        text(canvas, TelemetryFormat.grade(value.gradePercent), left + 18f, bottom - 15f, 18f, clay)
        text(canvas, "${value.tripMiles.toInt()} MI", left + 105f, bottom - 15f, 18f, muted)

        val graphLeft = left + 205f
        val graphRight = right - 18f
        val graphTop = top + 17f
        val graphBottom = bottom - 18f
        if (elevations.size > 1 && graphRight > graphLeft) {
            val points = elevations.toList()
            val low = points.min()
            val range = (points.max() - low).coerceAtLeast(80f)
            fun x(index: Int) = graphLeft + index * (graphRight - graphLeft) / (points.size - 1)
            fun y(elevation: Float) = graphBottom - (elevation - low) / range * (graphBottom - graphTop)
            val line = Path().apply {
                moveTo(x(0), y(points[0]))
                points.drop(1).forEachIndexed { index, elevation -> lineTo(x(index + 1), y(elevation)) }
            }
            val fill = Path(line).apply {
                lineTo(graphRight, graphBottom)
                lineTo(graphLeft, graphBottom)
                close()
            }
            canvas.drawPath(fill, paint(clay, alpha = 42))
            canvas.drawPath(line, paint(clay, stroke = 3f))
        }
    }

    private fun paint(color: Int, alpha: Int = 255, stroke: Float? = null) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.alpha = alpha
        if (stroke != null) {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }

    private fun text(canvas: Canvas, text: String, x: Float, y: Float, size: Float, color: Int) {
        canvas.drawText(text, x, y, paint(color).apply {
            textSize = size
            typeface = this@TelemetryOverlayView.typeface
        })
    }
}
