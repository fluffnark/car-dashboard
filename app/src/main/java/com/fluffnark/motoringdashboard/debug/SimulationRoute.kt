package com.fluffnark.motoringdashboard.debug

import com.fluffnark.motoringdashboard.data.DriveTelemetry

/** A deterministic US-550 route near Ouray for DHU and screenshot testing. */
object SimulationRoute {
    private data class Point(val lat: Double, val lon: Double, val elevation: Float, val heading: Float)
    private val route = listOf(
        Point(38.0350, -107.6717, 7792f, 180f),
        Point(38.0287, -107.6718, 7880f, 176f),
        Point(38.0228, -107.6714, 8025f, 168f),
        Point(38.0138, -107.6652, 8310f, 148f),
        Point(38.0047, -107.6570, 8620f, 160f),
        Point(37.9951, -107.6531, 8970f, 184f),
        Point(37.9852, -107.6577, 9340f, 205f),
        Point(37.9754, -107.6650, 9750f, 190f),
    )
    fun sample(step: Int): DriveTelemetry {
        val index = step.mod(route.size)
        val point = route[index]
        val previous = route[(index - 1).mod(route.size)]
        val speed = listOf(0f, 18f, 31f, 42f, 38f, 29f, 34f, 25f)[index]
        return DriveTelemetry(point.lat, point.lon, speed, point.heading, point.elevation,
            index * 1.15f, if (index == 0) 0f else (point.elevation - previous.elevation) / 6072f * 100f,
            if (index < 3) "Main Street · Ouray" else "US-550 · Million Dollar Highway", demo = true)
    }
    fun coordinates(): List<Pair<Double, Double>> = route.map { it.lon to it.lat }
}
