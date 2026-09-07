package com.fluffnark.motoringdashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fluffnark.motoringdashboard.data.DashboardFormat
import com.fluffnark.motoringdashboard.data.DashboardRepository
import com.fluffnark.motoringdashboard.data.DashboardState
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val Charcoal = Color(0xFF1C1E1F)
private val Parchment = Color(0xFFF1F2EE)
private val Persimmon = Color(0xFFE1A341)
private val Olive = Color(0xFFB1B8B5)
private val Dust = Color(0xFFA3A8AA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme { MotoringDashboard() }
        }
    }
}

@Composable
private fun MotoringDashboard() {
    val state by DashboardRepository.state.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(Charcoal).padding(horizontal = 24.dp, vertical = 34.dp)) {
        val isLandscape = maxWidth > maxHeight
        Box(Modifier.fillMaxSize()) {
            if (isLandscape) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    IdentityBlock(now, state, Modifier.weight(0.9f))
                    SpeedDial(state.speedMph, Modifier.weight(1.15f))
                    Readings(state, Modifier.weight(0.95f).verticalScroll(rememberScrollState()))
                }
            } else {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    IdentityBlock(now, state, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(28.dp))
                    SpeedDial(state.speedMph ?: state.rawSpeedMph, Modifier.weight(1f))
                    Spacer(Modifier.height(22.dp))
                    Readings(state, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun IdentityBlock(now: Long, state: DashboardState, modifier: Modifier = Modifier) {
    val instant = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
    Column(modifier, horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).background(if (state.connected) Olive else Persimmon, CircleShape))
            Text(
                if (state.connected) "  ANDROID AUTO · LIVE" else "  READY FOR ANDROID AUTO",
                color = Dust, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            instant.format(DateTimeFormatter.ofPattern("h:mm")),
            color = Parchment, fontSize = 58.sp, fontWeight = FontWeight.Light,
            fontFamily = FontFamily.SansSerif, letterSpacing = (-2).sp
        )
        Text(
            instant.format(DateTimeFormatter.ofPattern("EEEE · MMMM d")).uppercase(),
            color = Persimmon, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp
        )
        state.vehicleName?.let {
            Spacer(Modifier.height(10.dp))
            Text(it.uppercase(), color = Dust, fontSize = 11.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun SpeedDial(speed: Float?, modifier: Modifier = Modifier) {
    val displayed = DashboardFormat.speed(speed)
    Box(modifier.semantics { contentDescription = "$displayed miles per hour" }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(250.dp)) {
            val stroke = 12.dp.toPx()
            drawArc(
                color = Color(0xFF3B3932), startAngle = 140f, sweepAngle = 260f,
                useCenter = false, style = Stroke(stroke, cap = StrokeCap.Round),
                topLeft = Offset(stroke, stroke), size = Size(size.width - stroke * 2, size.height - stroke * 2)
            )
            if (speed != null) {
                drawArc(
                    color = Persimmon, startAngle = 140f,
                    sweepAngle = (speed.coerceIn(0f, 120f) / 120f) * 260f,
                    useCenter = false, style = Stroke(stroke, cap = StrokeCap.Round),
                    topLeft = Offset(stroke, stroke), size = Size(size.width - stroke * 2, size.height - stroke * 2)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(displayed, color = Parchment, fontSize = 78.sp, fontWeight = FontWeight.Light, fontFamily = FontFamily.SansSerif)
            Text("MILES PER HOUR", color = Dust, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
        }
    }
}

@Composable
private fun Readings(state: DashboardState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Reading("FUEL", DashboardFormat.percent(state.fuelPercent), Modifier.weight(1f))
            Reading("RANGE", DashboardFormat.miles(state.rangeMiles), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Reading("ODOMETER", DashboardFormat.miles(state.odometerMiles), Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        Text("SPEED · LAST 2 MINUTES", color = Dust, fontSize = 10.sp, letterSpacing = 1.sp)
        Canvas(Modifier.fillMaxWidth().height(54.dp).semantics {
            contentDescription = if (state.speedHistory.size < 2) "Waiting for speed history" else "Speed over the last two minutes"
        }) {
            drawLine(Dust.copy(alpha = 0.3f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            val end = state.speedHistory.lastOrNull()?.elapsedMillis ?: return@Canvas
            val ceiling = maxOf(60f, state.speedHistory.mapNotNull { it.mph }.maxOrNull() ?: 60f)
            state.speedHistory.zipWithNext().forEach { (a, b) ->
                val first = a.mph
                val second = b.mph
                if (first != null && second != null && b.elapsedMillis - a.elapsedMillis <= 5_000) {
                    fun x(time: Long) = ((time - end + 120_000) / 120_000f).coerceIn(0f, 1f) * size.width
                    drawLine(Persimmon, Offset(x(a.elapsedMillis), size.height * (1 - first / ceiling)),
                        Offset(x(b.elapsedMillis), size.height * (1 - second / ceiling)), 2.dp.toPx())
                }
            }
        }
        OutlinedButton(onClick = { ChatGptLauncher.open(context) }, modifier = Modifier.fillMaxWidth()) {
            Text("Start ChatGPT", color = Parchment)
        }
        if (!state.connected) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Connect to the Mazda display to request vehicle readings. Availability is decided by the car and Android Auto.",
                color = Dust, fontSize = 12.sp, lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun Reading(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Color(0xFF252829)).padding(12.dp)) {
        Text(label, color = Olive, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        Spacer(Modifier.height(5.dp))
        Text(value, modifier = Modifier.fillMaxWidth(), color = Parchment, fontSize = 25.sp,
            fontFamily = FontFamily.SansSerif, textAlign = TextAlign.Start)
    }
}
