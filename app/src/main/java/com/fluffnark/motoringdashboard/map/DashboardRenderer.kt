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
import com.fluffnark.motoringdashboard.data.ElevationSample
import com.fluffnark.motoringdashboard.data.TelemetryFormat
import com.fluffnark.motoringdashboard.data.TripElevationProfile
import com.fluffnark.motoringdashboard.data.VehicleData
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Low-frequency, allocation-light instrument artwork drawn directly to the car surface. */
class DashboardRenderer(context: Context) {
    private val typeface = ResourcesCompat.getFont(context, R.font.instrument_sans)
    private val boldTypeface = Typeface.create(typeface, Typeface.BOLD)
    private val elevationProfile = TripElevationProfile()

    @Synchronized
    fun record(telemetry: DriveTelemetry) {
        elevationProfile.record(telemetry)
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
        drawVehicleData(canvas, vehicle, width, palette, scale)
    }

    private fun drawBackdrop(canvas: Canvas, width: Float, height: Float, p: Palette, scale: Float) {
        val rule = paint(p.muted, alpha = if (p.dark) 48 else 54, stroke = 1.5f * scale)
        canvas.drawLine(width * .32f, height * .30f, width * .32f, height * .96f, rule)
        canvas.drawLine(width * .62f, height * .30f, width * .62f, height * .73f, rule)
        canvas.drawArc(RectF(width * .33f, height * .17f, width * .96f, height * 1.31f),
            198f, 112f, false, paint(p.accent, alpha = if (p.dark) 34 else 30, stroke = 2.5f * scale))
        canvas.drawRect(width * .34f, height * .065f, width * .405f, height * .074f, paint(p.accent))
        canvas.drawRect(width * .41f, height * .065f, width * .455f, height * .074f, paint(p.sage))
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
        centeredText(canvas, "0", cx - radius * .69f, cy + radius * .62f,
            10f * scale, p.muted, bold = true)
        centeredText(canvas, "50", cx, cy - radius * .74f,
            10f * scale, p.muted, bold = true)
        centeredText(canvas, "100", cx + radius * .69f, cy + radius * .62f,
            10f * scale, p.muted, bold = true)
    }

    private fun drawCompass(
        canvas: Canvas, heading: Float, cx: Float, cy: Float, radius: Float,
        p: Palette, scale: Float,
    ) {
        canvas.drawCircle(cx, cy, radius, paint(p.muted, alpha = 145, stroke = 3f * scale))
        canvas.drawCircle(cx, cy, radius - 7f * scale,
            paint(p.muted, alpha = 55, stroke = 1.2f * scale))
        repeat(24) { index ->
            val angle = radians(compassAngleDegrees(index * 15f, heading))
            val outer = radius
            val inner = radius - (if (index % 6 == 0) 10f else 5f) * scale
            canvas.drawLine(cx + cos(angle) * inner, cy + sin(angle) * inner,
                cx + cos(angle) * outer, cy + sin(angle) * outer,
                paint(p.muted, alpha = 185, stroke = 1.4f * scale))
        }
        listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f).forEach { (label, bearing) ->
            val angle = radians(compassAngleDegrees(bearing, heading))
            cardinal(canvas, label, cx + cos(angle) * radius * .69f,
                cy + sin(angle) * radius * .69f + 5f * scale,
                if (label == "N") p.accent else p.muted, scale)
        }

        // North rotates relative to the vehicle, whose forward direction is fixed at 12 o'clock.
        val angle = radians(compassAngleDegrees(0f, heading))
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
        val indexMark = Path().apply {
            moveTo(cx, cy - radius - 5f * scale)
            lineTo(cx - 5f * scale, cy - radius + 4f * scale)
            lineTo(cx + 5f * scale, cy - radius + 4f * scale)
            close()
        }
        canvas.drawPath(indexMark, paint(p.sage))
        centeredText(canvas, TelemetryFormat.heading(heading), cx, cy + radius + 26f * scale,
            16f * scale, p.primary, bold = true)
    }

    private fun drawClock(
        canvas: Canvas, time: LocalTime, cx: Float, cy: Float, radius: Float,
        p: Palette, scale: Float,
    ) {
        canvas.drawCircle(cx, cy, radius, paint(p.muted, alpha = 125, stroke = 3f * scale))
        canvas.drawCircle(cx, cy, radius - 7f * scale,
            paint(p.muted, alpha = 50, stroke = 1.2f * scale))
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
        repeat(4) { index ->
            val angle = radians(index * 90f - 90f)
            canvas.drawCircle(cx + cos(angle) * radius * .86f,
                cy + sin(angle) * radius * .86f, 2.4f * scale,
                paint(if (index == 0) p.accent else p.sage))
        }
        centeredText(canvas, time.format(CLOCK), cx, cy + radius + 26f * scale,
            18f * scale, p.primary, bold = true)
    }

    private fun drawElevation(
        canvas: Canvas, telemetry: DriveTelemetry, width: Float, height: Float,
        p: Palette, scale: Float,
    ) {
        val left = width * .02f
        val graphLeft = width * .235f
        val right = width * .965f
        val top = height * .765f
        val bottom = height * .91f
        val altitude = telemetry.elevationFeet?.let { "%,.0f".format(it) } ?: "—"
        text(canvas, altitude, left, top + 29f * scale, 30f * scale, p.primary, bold = true)
        text(canvas, "FT", left + measure(altitude, 31f * scale) + 6f * scale,
            top + 26f * scale, 12f * scale, p.muted, bold = true)
        text(canvas, formatDistance(telemetry.tripMiles), left,
            height * .905f, 17f * scale, p.primary, bold = true)
        text(canvas, TelemetryFormat.grade(telemetry.gradePercent), left,
            height * .965f, 14f * scale, p.accent, bold = true)

        val values = elevationProfile.snapshot()
        if (values.size < 2 || graphLeft >= right) return
        val rawLow = values.minOf { it.elevationFeet }
        val rawHigh = values.maxOf { it.elevationFeet }
        val interval = if (rawHigh - rawLow < 800f) 100f else 250f
        val low = floor((rawLow - 30f) / interval) * interval
        val high = ceil((rawHigh + 30f) / interval) * interval
        val distanceEnd = max(telemetry.tripMiles, values.last().distanceMiles).coerceAtLeast(.1f)
        drawGraphAxes(canvas, graphLeft, right, top, bottom, low, high, distanceEnd, p, scale)
        val path = graphPath(values, graphLeft, right, top, bottom, low, high, distanceEnd)
        val endpointX = projectX(values.last().distanceMiles, graphLeft, right, distanceEnd)
        val startX = projectX(values.first().distanceMiles, graphLeft, right, distanceEnd)
        val fill = Path(path).apply { lineTo(endpointX, bottom); lineTo(startX, bottom); close() }
        canvas.drawPath(fill, paint(p.accent, alpha = if (p.dark) 72 else 58))
        canvas.drawPath(path, paint(p.accent, stroke = 4.5f * scale))
        canvas.drawCircle(endpointX, projectY(values.last().elevationFeet, top, bottom, low, high),
            5f * scale, paint(p.accent))
    }

    private fun drawVehicleData(
        canvas: Canvas, vehicle: VehicleData, width: Float,
        p: Palette, scale: Float,
    ) {
        val values = buildList {
            vehicle.fuelPercent?.let { add("FUEL ${it.roundToInt()}%") }
            vehicle.rangeMiles?.let { add("${it.roundToInt()} MI RANGE") }
            vehicle.odometerMiles?.let { add("${"%,.0f".format(it)} MI") }
        }
        if (values.isEmpty()) return
        val label = values.joinToString("  ·  ")
        text(canvas, label, width * .47f, 34f * scale, 12f * scale, p.muted, bold = true)
    }

    private fun cardinal(canvas: Canvas, value: String, x: Float, y: Float, color: Int, scale: Float) =
        centeredText(canvas, value, x, y, 14f * scale, color, bold = true)

    private fun graphPath(
        values: List<ElevationSample>, left: Float, right: Float, top: Float, bottom: Float,
        low: Float, high: Float, distanceEnd: Float,
    ) = Path().apply {
        values.forEachIndexed { index, value ->
            val x = projectX(value.distanceMiles, left, right, distanceEnd)
            val y = projectY(value.elevationFeet, top, bottom, low, high)
            if (index == 0) moveTo(x, y) else lineTo(x, y)
        }
    }

    private fun drawGraphAxes(
        canvas: Canvas, left: Float, right: Float, top: Float, bottom: Float,
        low: Float, high: Float, distanceEnd: Float, p: Palette, scale: Float,
    ) {
        val axis = paint(p.muted, alpha = 135, stroke = 1.4f * scale)
        canvas.drawLine(left, top, left, bottom, axis)
        canvas.drawLine(left, bottom, right, bottom, axis)
        rightText(canvas, "${high.roundToInt()}", left - 7f * scale,
            top + 4f * scale, 10f * scale, p.muted, bold = true)
        rightText(canvas, "${low.roundToInt()}", left - 7f * scale,
            bottom + 3f * scale, 10f * scale, p.muted, bold = true)
        text(canvas, "FT", left + 6f * scale, top + 12f * scale,
            9f * scale, p.muted, bold = true)
        text(canvas, "0", left, bottom + 17f * scale, 10f * scale, p.muted, bold = true)
        centeredText(canvas, axisDistance(distanceEnd / 2f), (left + right) / 2f,
            bottom + 17f * scale, 10f * scale, p.muted, bold = true)
        rightText(canvas, "${axisDistance(distanceEnd)} MI", right,
            bottom + 17f * scale, 10f * scale, p.muted, bold = true)
    }

    private fun projectX(distance: Float, left: Float, right: Float, distanceEnd: Float): Float =
        left + (distance / distanceEnd).coerceIn(0f, 1f) * (right - left)

    private fun projectY(value: Float, top: Float, bottom: Float, low: Float, high: Float): Float =
        bottom - ((value - low) / (high - low).coerceAtLeast(.001f)).coerceIn(0f, 1f) * (bottom - top)

    private fun radians(degrees: Float): Float = Math.toRadians(degrees.toDouble()).toFloat()

    private fun formatDistance(miles: Float): String = "${axisDistance(miles)} MI"

    private fun axisDistance(miles: Float): String =
        if (miles < 10f) "%.1f".format(miles) else miles.roundToInt().toString()

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

    private fun rightText(
        canvas: Canvas, value: String, x: Float, y: Float, size: Float, color: Int,
        bold: Boolean = false,
    ) = canvas.drawText(value, x, y, paint(color).apply {
        textSize = size
        typeface = if (bold) boldTypeface else this@DashboardRenderer.typeface
        textAlign = Paint.Align.RIGHT
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

internal fun compassAngleDegrees(bearing: Float, heading: Float): Float = bearing - heading - 90f
