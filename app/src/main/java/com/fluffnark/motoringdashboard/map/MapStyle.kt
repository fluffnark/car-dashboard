package com.fluffnark.motoringdashboard.map

/** Original OpenMapTiles cartography. Data endpoint is isolated for a future offline provider. */
object MapStyle {
    const val TILE_SOURCE = "https://tiles.openfreemap.org/planet"
    enum class Look { WARM, SCANDINAVIAN, TECHNICAL }
    private data class Palette(val land: String, val park: String, val water: String, val minor: String,
        val major: String, val text: String, val accent: String)

    fun json(night: Boolean, look: Look = Look.WARM): String {
        val p = when (look to night) {
            Look.WARM to false -> Palette("#e8e2d5", "#cdd4b9", "#acc5c5", "#c8c3b5", "#faf3df", "#35372f", "#ad7445")
            Look.WARM to true -> Palette("#242521", "#30382d", "#283d43", "#55574e", "#b1a58a", "#ded5c1", "#c79560")
            Look.SCANDINAVIAN to false -> Palette("#e5e8e5", "#c9d5ce", "#a9c3cb", "#c1c7c4", "#f7f8f4", "#263236", "#65878a")
            Look.SCANDINAVIAN to true -> Palette("#202627", "#263431", "#20383e", "#465355", "#9dafac", "#dbe2df", "#729a9d")
            Look.TECHNICAL to false -> Palette("#dddcd7", "#ced2c9", "#aebbc1", "#b8b9b5", "#f1f0eb", "#25292c", "#9b7648")
            else -> Palette("#191c1e", "#222a26", "#1d3038", "#3c4448", "#879398", "#d2d7d7", "#b58a52")
        }
        val halo = p.land
        return """{
          "version":8,"name":"Motoring ${look.name}",
          "sources":{"streets":{"type":"vector","url":"$TILE_SOURCE"}},
          "glyphs":"https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf",
          "layers":[
            {"id":"land","type":"background","paint":{"background-color":"${p.land}"}},
            {"id":"park","type":"fill","source":"streets","source-layer":"landcover","paint":{"fill-color":"${p.park}","fill-opacity":0.6}},
            {"id":"water","type":"fill","source":"streets","source-layer":"water","paint":{"fill-color":"${p.water}"}},
            {"id":"minor-roads","type":"line","source":"streets","source-layer":"transportation","filter":["!in","class","motorway","trunk","primary"],"paint":{"line-color":"${p.minor}","line-width":["interpolate",["linear"],["zoom"],9,0.3,14,2,18,9]}},
            {"id":"major-casing","type":"line","source":"streets","source-layer":"transportation","filter":["in","class","motorway","trunk","primary"],"paint":{"line-color":"${p.accent}","line-width":["interpolate",["linear"],["zoom"],8,1.5,14,5,18,18]}},
            {"id":"major-roads","type":"line","source":"streets","source-layer":"transportation","filter":["in","class","motorway","trunk","primary"],"paint":{"line-color":"${p.major}","line-width":["interpolate",["linear"],["zoom"],8,0.7,14,3,18,14]}},
            {"id":"road-names","type":"symbol","source":"streets","source-layer":"transportation_name","minzoom":12,"layout":{"symbol-placement":"line","text-field":["coalesce",["get","name:en"],["get","name"],["get","ref"]],"text-font":["Noto Sans Regular"],"text-size":13,"symbol-spacing":350},"paint":{"text-color":"${p.text}","text-halo-color":"$halo","text-halo-width":1.5}},
            {"id":"places","type":"symbol","source":"streets","source-layer":"place","filter":["in","class","city","town","village"],"layout":{"text-field":["coalesce",["get","name:en"],["get","name"]],"text-font":["Noto Sans Regular"],"text-size":16,"text-padding":20},"paint":{"text-color":"${p.text}","text-halo-color":"$halo","text-halo-width":2}}
          ]}
        """.trimIndent()
    }
}
