package com.fluffnark.motoringdashboard.trip

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class TripItem(val id: String, val title: String, val done: Boolean)

data class TripChanges(val upserts: List<TripItem>, val deletions: Set<String>)

/** UI boundary shared by the phone editor, car template, and cloud reconciler. */
interface TripListStore {
    fun items(): List<TripItem>
    fun add(title: String)
    fun toggle(id: String)
    fun remove(id: String)
}

/** Small offline-first list with a durable journal for explicit Google Tasks sync. */
class TripListRepository(context: Context) : TripListStore {
    private val preferences = context.applicationContext
        .getSharedPreferences("trip_list", Context.MODE_PRIVATE)

    @Synchronized
    override fun items(): List<TripItem> = runCatching {
        val json = JSONArray(preferences.getString(KEY, "[]"))
        buildList {
            for (index in 0 until json.length()) {
                val value = json.getJSONObject(index)
                add(TripItem(value.getString("id"), value.getString("title"), value.optBoolean("done")))
            }
        }
    }.getOrDefault(emptyList())

    @Synchronized
    override fun add(title: String) {
        val clean = title.trim().replace(Regex("\\s+"), " ").take(80)
        if (clean.isEmpty()) return
        val item = TripItem("local:${UUID.randomUUID()}", clean, false)
        save(items() + item)
        markUpsert(item)
    }

    @Synchronized
    override fun toggle(id: String) {
        val next = items().map { if (it.id == id) it.copy(done = !it.done) else it }
        save(next)
        next.firstOrNull { it.id == id }?.let(::markUpsert)
    }

    @Synchronized
    override fun remove(id: String) {
        save(items().filterNot { it.id == id })
        val upserts = pendingChanges().upserts.filterNot { it.id == id }
        saveUpserts(upserts)
        if (id.startsWith("google:")) {
            preferences.edit { putString(DELETIONS, JSONArray(pendingChanges().deletions + id).toString()) }
        }
    }

    @Synchronized
    fun pendingChanges(): TripChanges {
        // v0.7 and earlier stored UUID items without a journal; preserve them on first cloud sync.
        val upserts = if (preferences.contains(UPSERTS)) readItems(UPSERTS)
            else items().filterNot { it.id.startsWith("google:") }
        val deletedJson = JSONArray(preferences.getString(DELETIONS, "[]"))
        val deletions = buildSet { for (index in 0 until deletedJson.length()) add(deletedJson.getString(index)) }
        return TripChanges(upserts, deletions)
    }

    @Synchronized
    fun replaceSynced(items: List<TripItem>) {
        save(items)
        preferences.edit { remove(UPSERTS); remove(DELETIONS) }
    }

    @Synchronized
    fun acknowledgeCreated(localId: String, remote: TripItem) {
        save(items().map { if (it.id == localId) remote else it })
        saveUpserts(pendingChanges().upserts.filterNot { it.id == localId } + remote)
    }

    private fun save(items: List<TripItem>) {
        preferences.edit { putString(KEY, encode(items).toString()) }
    }

    private fun markUpsert(item: TripItem) {
        saveUpserts(pendingChanges().upserts.filterNot { it.id == item.id } + item)
    }

    private fun saveUpserts(items: List<TripItem>) {
        preferences.edit { putString(UPSERTS, encode(items).toString()) }
    }

    private fun readItems(key: String): List<TripItem> = runCatching {
        val json = JSONArray(preferences.getString(key, "[]"))
        buildList {
            for (index in 0 until json.length()) {
                val value = json.getJSONObject(index)
                add(TripItem(value.getString("id"), value.getString("title"), value.optBoolean("done")))
            }
        }
    }.getOrDefault(emptyList())

    private fun encode(items: List<TripItem>): JSONArray {
        val json = JSONArray()
        items.forEach { item ->
            json.put(JSONObject().put("id", item.id).put("title", item.title).put("done", item.done))
        }
        return json
    }

    private companion object {
        const val KEY = "items"
        const val UPSERTS = "pending_upserts"
        const val DELETIONS = "pending_deletions"
    }
}
