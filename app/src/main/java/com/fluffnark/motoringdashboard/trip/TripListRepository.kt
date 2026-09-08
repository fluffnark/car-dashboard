package com.fluffnark.motoringdashboard.trip

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class TripItem(val id: String, val title: String, val done: Boolean)

/** Small offline-first list. A cloud provider can replace this without changing the car UI. */
class TripListRepository(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("trip_list", Context.MODE_PRIVATE)

    @Synchronized
    fun items(): List<TripItem> = runCatching {
        val json = JSONArray(preferences.getString(KEY, "[]"))
        buildList {
            for (index in 0 until json.length()) {
                val value = json.getJSONObject(index)
                add(TripItem(value.getString("id"), value.getString("title"), value.optBoolean("done")))
            }
        }
    }.getOrDefault(emptyList())

    @Synchronized
    fun add(title: String) {
        val clean = title.trim().replace(Regex("\\s+"), " ").take(80)
        if (clean.isEmpty()) return
        save(items() + TripItem(UUID.randomUUID().toString(), clean, false))
    }

    @Synchronized
    fun toggle(id: String) = save(items().map { if (it.id == id) it.copy(done = !it.done) else it })

    @Synchronized
    fun remove(id: String) = save(items().filterNot { it.id == id })

    private fun save(items: List<TripItem>) {
        val json = JSONArray()
        items.forEach { item ->
            json.put(JSONObject().put("id", item.id).put("title", item.title).put("done", item.done))
        }
        preferences.edit().putString(KEY, json.toString()).apply()
    }

    private companion object { const val KEY = "items" }
}
