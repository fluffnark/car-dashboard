package com.fluffnark.motoringdashboard.trip

import android.content.Context
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant

class GoogleTasksSync(context: Context) {
    private val repository = TripListRepository(context)

    suspend fun sync(accessToken: String): Int = withContext(Dispatchers.IO) {
        val api = GoogleTasksApi(accessToken)
        val listId = api.findOrCreateList("Motoring")
        val changes = repository.pendingChanges()
        changes.deletions.forEach { api.delete(listId, it.removePrefix("google:")) }
        changes.upserts.forEach { item ->
            if (item.id.startsWith("google:")) {
                api.update(listId, item.copy(id = item.id.removePrefix("google:")))
            } else {
                repository.acknowledgeCreated(item.id, api.create(listId, item))
            }
        }
        val current = api.tasks(listId)
        repository.replaceSynced(current)
        current.size
    }
}

internal class GoogleTasksApi(private val token: String) {
    fun findOrCreateList(title: String): String {
        val lists = request("GET", "/users/@me/lists?maxResults=100")
            .optJSONArray("items") ?: JSONArray()
        for (index in 0 until lists.length()) {
            val list = lists.getJSONObject(index)
            if (list.optString("title") == title) return list.getString("id")
        }
        return request("POST", "/users/@me/lists", JSONObject().put("title", title)).getString("id")
    }

    fun tasks(listId: String): List<TripItem> {
        val result = request("GET", "/lists/${encode(listId)}/tasks?showCompleted=true&showHidden=true&maxResults=100")
        val values = result.optJSONArray("items") ?: JSONArray()
        return buildList {
            for (index in 0 until values.length()) {
                val task = values.getJSONObject(index)
                if (!task.optBoolean("deleted") && task.optString("title").isNotBlank()) {
                    add(TripItem("google:${task.getString("id")}", task.getString("title").take(80), task.optString("status") == "completed"))
                }
            }
        }
    }

    fun create(listId: String, item: TripItem): TripItem {
        val created = request("POST", "/lists/${encode(listId)}/tasks", body(item))
        return item.copy(id = "google:${created.getString("id")}")
    }

    fun update(listId: String, item: TripItem) {
        request("PATCH", "/lists/${encode(listId)}/tasks/${encode(item.id)}", body(item))
    }

    fun delete(listId: String, taskId: String) {
        request("DELETE", "/lists/${encode(listId)}/tasks/${encode(taskId)}")
    }

    private fun body(item: TripItem) = JSONObject()
        .put("title", item.title)
        .put("status", if (item.done) "completed" else "needsAction")
        .put("completed", if (item.done) Instant.now().toString() else JSONObject.NULL)

    private fun request(method: String, path: String, body: JSONObject? = null): JSONObject {
        val connection = URI("https://tasks.googleapis.com/tasks/v1$path").toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
            }
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_NO_CONTENT) return JSONObject()
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = runCatching { JSONObject(response).getJSONObject("error").getString("message") }.getOrDefault("HTTP $code")
                error(message)
            }
            return if (response.isBlank()) JSONObject() else JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}

internal fun googleTasksErrorLabel(error: Throwable): String {
    val message = error.message.orEmpty()
    return when {
        (error as? ApiException)?.statusCode == 10 -> "OAUTH CLIENT MISMATCH"
        message.contains("verification process", ignoreCase = true) ||
            message.contains("access_denied", ignoreCase = true) -> "ADD OAUTH TEST USER"
        message.contains("Tasks API", ignoreCase = true) &&
            message.contains("disabled", ignoreCase = true) -> "ENABLE TASKS API"
        message.contains("insufficient authentication", ignoreCase = true) -> "CHECK TASKS SCOPE"
        message.contains("401") -> "RECONNECT GOOGLE"
        else -> message.take(42).ifBlank { "SYNC FAILED" }
    }
}
