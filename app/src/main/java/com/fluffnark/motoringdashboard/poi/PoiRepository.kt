package com.fluffnark.motoringdashboard.poi

import kotlin.math.*

enum class PoiCategory { FUEL, PARKING, VIEWPOINT, TRAILHEAD, REST_AREA, EMERGENCY }
data class Poi(val name: String, val category: PoiCategory, val latitude: Double, val longitude: Double, val detail: String)

object PoiRepository {
    val demo = listOf(
        Poi("Ouray Visitor Center", PoiCategory.PARKING, 38.0211, -107.6721, "Parking · restrooms"),
        Poi("Box Cañon Falls", PoiCategory.VIEWPOINT, 38.0170, -107.6769, "Scenic viewpoint"),
        Poi("Bear Creek Falls", PoiCategory.VIEWPOINT, 37.9923, -107.6545, "Roadside overlook"),
        Poi("Ouray Fuel", PoiCategory.FUEL, 38.0310, -107.6717, "Regular fuel"),
        Poi("Ouray County Hospital", PoiCategory.EMERGENCY, 38.0244, -107.6700, "Emergency care"),
        Poi("Perimeter Trail", PoiCategory.TRAILHEAD, 38.0196, -107.6680, "Trailhead"),
    )
    fun nearby(latitude: Double, longitude: Double, limit: Int = 5): List<Pair<Poi, Double>> = demo
        .map { it to miles(latitude, longitude, it.latitude, it.longitude) }
        .filter { it.second <= 75.0 }
        .sortedBy { it.second }
        .take(limit)
    fun miles(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1); val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 3958.8 * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
