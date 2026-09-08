package com.fluffnark.motoringdashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fluffnark.motoringdashboard.map.MapPreferences
import com.fluffnark.motoringdashboard.map.MapStyle

private val Graphite = Color(0xFF20211E)
private val Chalk = Color(0xFFE7E2D6)
private val Clay = Color(0xFFB77949)
private val Sage = Color(0xFFAEB7A4)
private val Muted = Color(0xFF9A9D95)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MaterialTheme { SetupScreen() } }
    }
}

@Composable
private fun SetupScreen() {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(MapPreferences.getLook(context)) }
    Column(
        Modifier.fillMaxSize().background(Graphite).verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 42.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(Sage, CircleShape))
            Text("  ANDROID AUTO · READY", color = Muted, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
        }
        Spacer(Modifier.height(24.dp))
        Text("Motoring", color = Chalk, fontSize = 48.sp, fontWeight = FontWeight.Light,
            letterSpacing = (-1.5).sp)
        Text("ROAD ATLAS", color = Clay, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp)
        Spacer(Modifier.height(30.dp))
        TerrainPreview()
        Spacer(Modifier.height(28.dp))
        Text("A calm map, useful places, and honest drive telemetry for the Mazda display.",
            color = Chalk, fontSize = 18.sp, lineHeight = 26.sp)
        Spacer(Modifier.height(30.dp))
        Text("CARTOGRAPHY", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MapStyle.Look.entries.forEach { look ->
                PaletteChoice(look, selected == look, Modifier.weight(1f)) {
                    selected = look
                    MapPreferences.setLook(context, look)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        SetupLine("01", "Install this build from the Play internal-test track")
        SetupLine("02", "Enable Motoring Dashboard in Customize launcher")
        SetupLine("03", "Connect USB and allow location and vehicle data")
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = { ChatGptLauncher.open(context) }, modifier = Modifier.fillMaxWidth()) {
            Text("Start ChatGPT voice", color = Chalk)
        }
        Spacer(Modifier.height(12.dp))
        Text("The car action is parked-only. Enable Background conversations in ChatGPT Voice settings if you want the conversation to continue after returning to the map.",
            color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun TerrainPreview() {
    Canvas(Modifier.fillMaxWidth().height(150.dp).background(Color(0xFF292B27))) {
        repeat(5) { index ->
            val path = Path().apply {
                moveTo(0f, size.height * (0.25f + index * 0.12f))
                cubicTo(size.width * .25f, size.height * (.05f + index * .15f),
                    size.width * .55f, size.height * (.65f + index * .04f),
                    size.width, size.height * (.22f + index * .13f))
            }
            drawPath(path, if (index == 2) Clay else Sage.copy(alpha = .35f),
                style = Stroke(if (index == 2) 3.dp.toPx() else 1.dp.toPx()))
        }
        drawCircle(Chalk, 7.dp.toPx(), Offset(size.width * .57f, size.height * .54f))
        drawCircle(Graphite, 3.dp.toPx(), Offset(size.width * .57f, size.height * .54f))
    }
}

@Composable
private fun PaletteChoice(look: MapStyle.Look, selected: Boolean, modifier: Modifier, choose: () -> Unit) {
    val accent = when (look) {
        MapStyle.Look.WARM -> Clay
        MapStyle.Look.SCANDINAVIAN -> Color(0xFF78999B)
        MapStyle.Look.TECHNICAL -> Color(0xFFB08A59)
    }
    Column(modifier.clickable(onClick = choose).padding(vertical = 8.dp)) {
        Box(Modifier.fillMaxWidth().height(5.dp).background(if (selected) accent else Color(0xFF4A4D48)))
        Spacer(Modifier.height(8.dp))
        Text(look.name.lowercase().replaceFirstChar(Char::uppercase),
            color = if (selected) Chalk else Muted, fontSize = 12.sp)
    }
}

@Composable
private fun SetupLine(number: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
        Text(number, color = Clay, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.width(38.dp))
        Text(text, color = Chalk, fontSize = 14.sp, lineHeight = 20.sp)
    }
}
