package com.fluffnark.motoringdashboard.map

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.fluffnark.motoringdashboard.R
import com.fluffnark.motoringdashboard.data.DriveTelemetry
import com.fluffnark.motoringdashboard.data.TelemetryFormat
import com.fluffnark.motoringdashboard.data.VehicleData
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Low-frequency, allocation-light instrument artwork drawn directly to the car surface. */
class DashboardRenderer(context: Context) {
    private val typeface = ResourcesCompat.getFont(context, R.font.instrument_sans)
    private val boldTypeface = Typeface.create(typeface, Typeface.BOLD)
    private val elevations = ArrayDeque<Float>()
    private var lastTripMiles: Float? = null

    @Synchronized
    fun record(telemetry: DriveTelemetry) {
        if (telemetry.demo && lastTripMiles?.let { telemetry.tripMiles < it } == true) elevations.clear()
        telemetry.elevationFeet?.let(elevations::addLast)
        while (elevations.size > 72) elevations.removeFirst()
        lastTripMiles = telemetry.tripMiles
    }

    @Synchronized
    fun draw(
        canvas: Canvas,
        telemetry: DriveTelemetry,
        vehicle: VehicleData,
        night: Boolean,
        now: LocalTime = LocalTime.now(),
    ) {
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()
        val scale = min(width / 941f, height / 423f).coerceIn(.68f, 1.55f)
        val palette = if (night) Palette.night else Palette.day

        canvas.drawColor(palette.background)
        drawBackdrop(canvas, width, height, palette, scale)

        val speed = vehicle.speedMph ?: telemetry.speedMph
        drawSpeed(canvas, speed, width * .20f, height * .49f, height * .245f,
            palette, scale)
        drawCompass(canvas, telemetry.headingDegrees, width * .49f, height * .49f,
            height * .175f, palette, scale)
        drawClock(canvas, now, width * .75f, height * .49f, height * .175f,
            palette, scale)
        drawElevation(canvas, telemetry, width, height, palette, scale)
        drawGpsAccuracy(canvas, telemetry.accuracyFeet, width, height, palette, scale)
        drawVehicleData(canvas, vehicle, width, height, palette, scale)
    }

    private fun drawBackdrop(canvas: Canvas, width: Float, height: Float, p: Palette, scale: Float) {
        val rule = paint(p.muted, alpha = if (p.dark) 48 else 54, stroke = 1.5f * scale)
        canvas.drawLine(width * .32f, height * .30f, width * .32f, height * .96f, rule)
        canvas.drawLine(width * .62f, height * .30f, width * .62f, height * .73f, rule)
        canvas.drawArc(RectF(width * .33f, height * .17f, width * .96f, height * 1.31f),
            198f, 112f, false, paint(p.accent, alpha = if (p.dark) 34 else 30, stroke = 2.5f * scale))
    }

    private fun drawSpeed(
        canvas: Canvas, speed: Float, cx: Float, cy: Float, radius: Float,
        p: Palette, scale: Float,
    ) {
        val start = 135f
        val sweep = 270f
        val fraction = (speed / 100f).coerceIn(0f, 1f)
        val bounds = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(bounds, start, sweep, false, paint(p.muted, alpha = 110, stroke = 8f * scale))
        canvas.drawArc(bounds, start, sweep * fraction, false, paint(p.sage, stroke = 8f * scale))

        repeat(21) { index ->
            val angle = radians(start + sweep * index / 20f)
            val outer = radius + 1f * scale
            val inner = radius - (if (index % 2 == 0) 12f else 6f) * scale
            canvas.drawLine(cx + cos(angle) * inner, cy + sin(angle) * inner,
                cx + cos(angle) * outer, cy + sin(angle) * outer,
                paint(p.muted, alpha = 205,
                    stroke = if (index % 2 == 0) 2.6f * scale else 1.5f * scale))
        }

        val angle = radians(start + sweep * fraction)
        canvas.drawLine(cx + cos(angle) * radius * .70f, cy + sin(angle) * radius * .70f,
            cx + cos(angle) * radius * .90f, cy + sin(angle) * radius * .90f,
            paint(p.sage, stroke = 5f * scale))
        centeredText(canvas, speed.roundToInt().toString(), cx, cy + 20f * scale,
            72f * scale, p.primary, bold = true)
        centeredText(canvas, "MPH", cx, cy + 48f * scale, 15f * scale, p.muted, bold = true)
    }

    private fun drawCompass(
        canvas: Canvas, heading: Float, cx: Float, cy: Float, radius: Float,
        p: Palette, scale: Float,
    ) {
        canvas.drawCircle(cx, cy, radius, paint(p.muted, alpha = 125, stroke = 3f * scale))
        repeat(24) { index ->
            val angle = radians(-90f + index * 15f)
            val outer = radius
            val inner = radius - (if (index % 6 == 0) 10f else 5f) * scale
            canvas.drawLine(cx + cos(angle) * inner, cy + sin(angle) * inner,
                cx + cos(angle) * outer, cy + sin(angle) * outer,
                paint(p.muted, alpha = 185, stroke = 1.4f * scale))
        }
        cardinal(canvas, "N", cx, cy - radius * .69f, p.accent, scale)
        cardinal(canvas, "E", cx + radius * .69f, cy + 5f * scale, p.muted, scale)
        cardinal(canvas, "S", cx, cy + radius * .78f, p.muted, scale)
        cardinal(canvas, "W", cx - radius * .69f, cy + 5f * scale, p.muted, scale)

        val angle = radians(heading - 90f)
        val side = radius * .11f
        val tipX = cx + cos(angle) * radius * .58f
        val tipY = cy + sin(angle) * radius * .58f
        val tailX = cx - cos(angle) * radius * .34f
        val tailY = cy - sin(angle) * radius * .34f
        val arrow = Path().apply {
            moveTo(tipX, tipY)
            lineTo(cx + cos(angle + Math.PI.toFloat() / 2f) * side,
                cy + sin(angle + Math.PI.toFloat() / 2f) * side)
            lineTo(tailX, tailY)
            lineTo(cx + cos(angle - Math.PI.toFloat() / 2f) * side,
                cy + sin(angle - Math.PI.toFloat() / 2f) * side)
            close()
        }
        canvas.drawPath(arrow, paint(p.accent))
        canvas.drawCircle(cx, cy, 4f * scale, paint(p.primary))
        centeredText(canvas, TelemetryFormat.heading(heading), cx, cy + radius + 26f * scale,
            16f * scale, p.primary, bold = true)
    }

    private fun drawClock(
        canvas: Canvas, time: LocalTime, cx: Float, cy: Float, radius: Float,
        p: Palette, scale: Float,
    ) {
        canvas.drawCircle(cx, cy, radius, paint(p.muted, alpha = 125, stroke = 3f * scale))
        repeat(12) { index ->
            val angle = radians(-90f + index * 30f)
            val inner = radius - (if (index % 3 == 0) 10f else 6f) * scale
            canvas.drawLine(cx + cos(angle) * inner, cy + sin(angle) * inner,
                cx + cos(angle) * radius, cy + sin(angle) * radius,
                paint(p.muted, alpha = 190,
                    stroke = if (index % 3 == 0) 2.4f * scale else 1.4f * scale))
        }
        val minuteAngle = radians(time.minute * 6f - 90f)
        val hourAngle = radians((time.hour % 12 + time.minute / 60f) * 30f - 90f)
        canvas.drawLine(cx, cy, cx + cos(hourAngle) * radius * .48f,
            cy + sin(hourAngle) * radius * .48f, paint(p.primary, stroke = 5f * scale))
        canvas.drawLine(cx, cy, cx + cos(minuteAngle) * radius * .72f,
            cy + sin(minuteAngle) * radius * .72f, paint(p.accent, stroke = 3f * scale))
        canvas.drawCircle(cx, cy, 5f * scale, paint(p.primary))
        centeredText(canvas, time.format(CLOCK), cx, cy + radius + 26f * scale,
            18f * scale, p.primary, bold = true)
    }

    private fun drawElevation(
        canvas: Canvas, telemetry: DriveTelemetry, width: Float, height: Float,
        p: Palette, scale: Float,
    ) {
        val left = width * .37f
        val graphLeft = width * .57f
        val right = width * .965f
        val top = height * .79f
        val bottom = height * .95f
        val altitude = telemetry.elevationFeet?.let { "%,.0f".format(it) } ?: "—"
        text(canvas, altitude, left, top + 30f * scale, 31f * scale, p.primary, bold = true)
        text(canvas, "FT", left + measure(altitude, 31f * scale) + 6f * scale,
            top + 27f * scale, 12f * scale, p.muted, bold = true)
        text(canvas, TelemetryFormat.grade(telemetry.gradePercent), left,
            bottom, 15f * scale, p.accent, bold = true)
        text(canvas, "${telemetry.tripMiles.roundToInt()} MI", left + 73f * scale,
            bottom, 15f * scale, p.muted, bold = true)

        val values = elevations.toList()
        if (values.size < 2 || graphLeft >= right) return
        val low = values.min()
        val high = values.max()
        val padding = max(60f, (high - low) * .12f)
        val path = graphPath(values, graphLeft, right, top, bottom, low - padding, high + padding)
        val fill = Path(path).apply { lineTo(right, bottom); lineTo(graphLeft, bottom); close() }
        canvas.drawPath(fill, paint(p.accent, alpha = 62))
        canvas.drawPath(path, paint(p.accent, stroke = 4.5f * scale))
        canvas.drawCircle(right, projectY(values.last(), top, bottom, low - padding, high + padding),
            5f * scale, paint(p.accent))
    }

    private fun drawGpsAccuracy(
        canvas: Canvas, accuracyFeet: Float?, width: Float, height: Float,
        p: Palette, scale: Float,
    ) {
        val accuracy = accuracyFeet ?: return
        val cx = width * .055f
        val cy = height * .835f
        repeat(3) { index ->
            val radius = (7f + index * 7f) * scale
            canvas.drawArc(RectF(cx - radius, cy - radius, cx + radius, cy + radius),
                205f, 130f, false, paint(if (index == 0) p.sage else p.muted,
                    alpha = 210 - index * 34, stroke = 2.2f * scale))
        }
        canvas.drawCircle(cx, cy, 3.5f * scale, paint(p.sage))
        text(canvas, "±${accuracy.roundToInt()} FT", width * .092f, cy + 5f * scale,
            14f * scale, p.primary, bold = true)
    }

    private fun drawVehicleData(
        canvas: Canvas, vehicle: VehicleData, width: Float, height: Float,
        p: Palette, scale: Float,
    ) {
        val values = buildList {
            vehicle.fuelPercent?.let { add("FUEL ${it.roundToInt()}%") }
            vehicle.rangeMiles?.let { add("${it.roundToInt()} MI RANGE") }
            vehicle.odometerMiles?.let { add("${"%,.0f".format(it)} MI") }
        }
        if (values.isEmpty()) return
        val label = values.joinToString("  ·  ")
        text(canvas, label, width * .02f, height * .96f, 14f * scale, p.muted, bold = true)
    }

    private fun cardinal(canvas: Canvas, value: String, x: Float, y: Float, color: Int, scale: Float) =
        centeredText(canvas, value, x, y, 14f * scale, color, bold = true)

    private fun graphPath(
        values: List<Float>, left: Float, right: Float, top: Float, bottom: Float,
        low: Float, high: Float,
    ) = Path().apply {
        values.forEachIndexed { index, value ->
            val x = left + index * (right - left) / (values.size - 1)
            val y = projectY(value, top, bottom, low, high)
            if (index == 0) moveTo(x, y) else lineTo(x, y)
        }
    }

    private fun projectY(value: Float, top: Float, bottom: Float, low: Float, high: Float): Float =
        bottom - ((value - low) / (high - low).coerceAtLeast(.001f)).coerceIn(0f, 1f) * (bottom - top)

    private fun radians(degrees: Float): Float = Math.toRadians(degrees.toDouble()).toFloat()

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

    private fun text(
        canvas: Canvas, value: String, x: Float, y: Float, size: Float, color: Int,
        bold: Boolean = false,
    ) = canvas.drawText(value, x, y, paint(color).apply {
        textSize = size
        typeface = if (bold) boldTypeface else this@DashboardRenderer.typeface
    })

    private fun centeredText(
        canvas: Canvas, value: String, x: Float, y: Float, size: Float, color: Int,
        bold: Boolean = false,
    ) =
        canvas.drawText(value, x, y, paint(color).apply {
            textSize = size
            typeface = if (bold) boldTypeface else this@DashboardRenderer.typeface
            textAlign = Paint.Align.CENTER
        })

    private fun measure(value: String, size: Float) = paint(Color.WHITE).apply {
        textSize = size
        typeface = this@DashboardRenderer.typeface
    }.measureText(value)

    private data class Palette(
        val background: Int,
        val primary: Int,
        val muted: Int,
        val accent: Int,
        val sage: Int,
        val dark: Boolean,
    ) {
        companion object {
            val day = Palette(Color.rgb(225, 222, 211), Color.rgb(26, 29, 27),
                Color.rgb(77, 81, 75), Color.rgb(164, 86, 43), Color.rgb(61, 112, 94), false)
            val night = Palette(Color.rgb(18, 21, 19), Color.rgb(247, 242, 230),
                Color.rgb(187, 191, 181), Color.rgb(220, 157, 93), Color.rgb(155, 194, 174), true)
        }
    }

    private companion object {
        val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm")
    }
}
