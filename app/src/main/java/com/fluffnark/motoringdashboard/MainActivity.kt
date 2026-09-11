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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.fluffnark.motoringdashboard.trip.TripListRepository

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
    val tripList = remember { TripListRepository(context) }
    var tripItems by remember { mutableStateOf(tripList.items()) }
    var newItem by remember { mutableStateOf("") }
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
        Text("DRIVE INSTRUMENTS", color = Clay, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp)
        Spacer(Modifier.height(30.dp))
        InstrumentPreview()
        Spacer(Modifier.height(28.dp))
        Text("Calm, glanceable drive telemetry for the Mazda display.",
            color = Chalk, fontSize = 18.sp, lineHeight = 26.sp)
        Spacer(Modifier.height(30.dp))
        Text("TRIP LIST", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newItem,
                onValueChange = { newItem = it.take(80) },
                placeholder = { Text("Add item", color = Muted) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = {
                tripList.add(newItem)
                newItem = ""
                tripItems = tripList.items()
            }) { Text("+", color = Clay, fontSize = 28.sp) }
        }
        tripItems.forEach { item ->
            Row(
                Modifier.fillMaxWidth().clickable {
                    tripList.toggle(item.id)
                    tripItems = tripList.items()
                }.padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (item.done) "✓" else "○", color = if (item.done) Sage else Clay,
                    fontSize = 20.sp, modifier = Modifier.width(34.dp))
                Text(item.title, color = if (item.done) Muted else Chalk, fontSize = 15.sp,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    tripList.remove(item.id)
                    tripItems = tripList.items()
                }) { Text("×", color = Muted, fontSize = 20.sp) }
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
        Text("The car action is parked-only. Enable Background conversations in ChatGPT Voice settings if you want the conversation to continue after returning to the instruments.",
            color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun InstrumentPreview() {
    Canvas(Modifier.fillMaxWidth().height(150.dp).background(Color(0xFF292B27))) {
        val radius = size.height * .31f
        listOf(.19f, .5f, .81f).forEachIndexed { index, fraction ->
            drawCircle(if (index == 0) Sage else Muted.copy(alpha = .55f), radius,
                Offset(size.width * fraction, size.height * .49f), style = Stroke(if (index == 0) 3.dp.toPx() else 1.dp.toPx()))
        }
        val profile = Path().apply {
            moveTo(size.width * .58f, size.height * .80f)
            cubicTo(size.width * .68f, size.height * .76f, size.width * .78f,
                size.height * .58f, size.width * .94f, size.height * .63f)
        }
        drawPath(profile, Clay, style = Stroke(3.dp.toPx()))
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
