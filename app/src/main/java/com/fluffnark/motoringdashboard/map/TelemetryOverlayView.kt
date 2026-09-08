package com.fluffnark.motoringdashboard.map

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.fluffnark.motoringdashboard.R
import com.fluffnark.motoringdashboard.data.DriveTelemetry
import com.fluffnark.motoringdashboard.data.TelemetryFormat
import kotlin.math.max
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** A compact instrument layer: altitude, speed dial, and elevation profile. */
class TelemetryOverlayView(context: Context) : View(context) {
    private val typeface = ResourcesCompat.getFont(context, R.font.instrument_sans)
    private val samples = ArrayDeque<DriveTelemetry>()
    private var current: DriveTelemetry? = null
    var night: Boolean = false
        set(value) { field = value; invalidate() }

    init {
        isClickable = false
        isFocusable = false
    }

    fun update(value: DriveTelemetry) {
        if (value.demo && current?.let { value.tripMiles < it.tripMiles } == true) samples.clear()
        current = value
        samples.addLast(value)
        while (samples.size > 72) samples.removeFirst()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val value = current ?: return
        val scale = (width / 800f).coerceIn(.78f, 1.35f)
        val left = 14f * scale
        val right = width - max(102f * scale, width * .13f)
        val bottom = height - 14f * scale
        val panelHeight = (132f * scale).coerceAtMost(height * .38f)
        val top = bottom - panelHeight
        if (right - left < 360f * scale || panelHeight < 96f) return

        canvas.drawRoundRect(left, top, right, bottom, 9f * scale, 9f * scale, paint(
            if (night) Color.rgb(27, 29, 27) else Color.rgb(38, 40, 37), alpha = 226,
        ))
        val chalk = Color.rgb(239, 234, 222)
        val muted = Color.rgb(171, 175, 166)
        val clay = if (night) Color.rgb(211, 162, 107) else Color.rgb(199, 132, 78)
        val sage = if (night) Color.rgb(151, 174, 158) else Color.rgb(165, 191, 174)

        val metricLeft = left + 17f * scale
        val altitude = value.elevationFeet?.let { "%,.0f".format(it) } ?: "—"
        text(canvas, altitude, metricLeft, top + 61f * scale, 43f * scale, chalk)
        text(canvas, "FT", metricLeft + measure(altitude, 43f * scale) + 7f * scale,
            top + 56f * scale, 13f * scale, muted)
        text(canvas, TelemetryFormat.grade(value.gradePercent), metricLeft,
            bottom - 20f * scale, 16f * scale, clay)
        text(canvas, "${value.tripMiles.toInt()} MI", metricLeft + 82f * scale,
            bottom - 20f * scale, 16f * scale, muted)

        val dialX = left + 246f * scale
        val dialY = top + 66f * scale
        speedDial(canvas, value.speedMph, dialX, dialY, 45f * scale, sage, muted, chalk, scale)

        val graphLeft = left + 306f * scale
        val graphRight = right - 16f * scale
        if (graphRight <= graphLeft) return
        val values = samples.toList()

        label(canvas, "ELEV", graphLeft, top + 19f * scale, muted, scale)
        elevationGraph(canvas, values, graphLeft, graphRight,
            top + 27f * scale, bottom - 17f * scale, clay, scale)
    }

    private fun elevationGraph(
        canvas: Canvas, values: List<DriveTelemetry>, left: Float, right: Float,
        top: Float, bottom: Float, color: Int, scale: Float,
    ) {
        val elevations = values.mapNotNull { it.elevationFeet }
        if (elevations.size < 2) return
        val low = elevations.min()
        val high = elevations.max()
        val padding = max(70f, (high - low) * .12f)
        val path = graphPath(elevations, left, right, top, bottom, low - padding, high + padding)
        val fill = Path(path).apply { lineTo(right, bottom); lineTo(left, bottom); close() }
        canvas.drawPath(fill, paint(color, alpha = 46))
        canvas.drawPath(path, paint(color, stroke = 3f * scale))
        drawEndpoint(canvas, elevations.last(), right, top, bottom, low - padding, high + padding, color, scale)
    }

    private fun speedDial(
        canvas: Canvas, speed: Float, centerX: Float, centerY: Float, radius: Float,
        color: Int, muted: Int, chalk: Int, scale: Float,
    ) {
        val start = 135f
        val sweep = 270f
        val fraction = (speed / 100f).coerceIn(0f, 1f)
        val bounds = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        canvas.drawArc(bounds, start, sweep, false, paint(muted, alpha = 65, stroke = 4f * scale))
        canvas.drawArc(bounds, start, sweep * fraction, false, paint(color, stroke = 4f * scale))

        repeat(11) { index ->
            val angle = Math.toRadians((start + sweep * index / 10f).toDouble())
            val outer = radius + 1f * scale
            val inner = radius - (if (index % 2 == 0) 7f else 4f) * scale
            canvas.drawLine(
                centerX + cos(angle).toFloat() * inner,
                centerY + sin(angle).toFloat() * inner,
                centerX + cos(angle).toFloat() * outer,
                centerY + sin(angle).toFloat() * outer,
                paint(muted, alpha = 135, stroke = 1.4f * scale),
            )
        }

        val needleAngle = Math.toRadians((start + sweep * fraction).toDouble())
        canvas.drawLine(
            centerX + cos(needleAngle).toFloat() * radius * .48f,
            centerY + sin(needleAngle).toFloat() * radius * .48f,
            centerX + cos(needleAngle).toFloat() * radius * .78f,
            centerY + sin(needleAngle).toFloat() * radius * .78f,
            paint(color, stroke = 2.5f * scale),
        )
        centeredText(canvas, speed.roundToInt().toString(), centerX, centerY + 10f * scale,
            34f * scale, chalk)
        centeredText(canvas, "MPH", centerX, centerY + 29f * scale,
            10f * scale, muted)
    }

    private fun graphPath(
        values: List<Float>, left: Float, right: Float, top: Float, bottom: Float,
        low: Float, high: Float,
    ): Path = Path().apply {
        values.forEachIndexed { index, value ->
            val x = left + index * (right - left) / (values.size - 1)
            val y = projectY(value, top, bottom, low, high)
            if (index == 0) moveTo(x, y) else lineTo(x, y)
        }
    }

    private fun drawEndpoint(
        canvas: Canvas, value: Float, right: Float, top: Float, bottom: Float,
        low: Float, high: Float, color: Int, scale: Float,
    ) {
        canvas.drawCircle(right, projectY(value, top, bottom, low, high),
            3.4f * scale, paint(color))
    }

    private fun projectY(value: Float, top: Float, bottom: Float, low: Float, high: Float): Float {
        val fraction = ((value - low) / (high - low).coerceAtLeast(.001f)).coerceIn(0f, 1f)
        return bottom - fraction * (bottom - top)
    }

    private fun label(canvas: Canvas, value: String, x: Float, y: Float, color: Int, scale: Float) =
        text(canvas, value, x, y, 11.5f * scale, color)

    private fun paint(color: Int, alpha: Int = 255, stroke: Float? = null) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
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

    private fun centeredText(canvas: Canvas, text: String, x: Float, y: Float, size: Float, color: Int) {
        canvas.drawText(text, x, y, paint(color).apply {
            textSize = size
            typeface = this@TelemetryOverlayView.typeface
            textAlign = Paint.Align.CENTER
        })
    }

    private fun measure(text: String, size: Float): Float = paint(Color.WHITE).apply {
        textSize = size
        typeface = this@TelemetryOverlayView.typeface
    }.measureText(text)
}
