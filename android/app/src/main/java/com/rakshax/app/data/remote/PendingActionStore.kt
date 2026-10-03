package com.rakshax.app.data.remote

import android.content.Context
import com.rakshax.app.data.secure.SecureStore
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class PendingAction(
    val id: String = UUID.randomUUID().toString(),
    val path: String,
    val body: String
)

class PendingActionStore(context: Context) {
    private val secureStore = SecureStore(context)

    @Synchronized
    fun enqueue(action: PendingAction) {
        val actions = read().toMutableList()
        actions += action
        secureStore.putString(KEY, JSONArray().apply {
            actions.takeLast(MAX_ACTIONS).forEach {
                put(JSONObject().apply {
                    put("id", it.id)
                    put("path", it.path)
                    put("body", it.body)
                })
            }
        }.toString())
    }

    @Synchronized
    fun all(): List<PendingAction> = read()

    @Synchronized
    fun remove(id: String) {
        val remaining = read().filterNot { it.id == id }
        secureStore.putString(KEY, JSONArray().apply {
            remaining.forEach {
                put(JSONObject().apply {
                    put("id", it.id)
                    put("path", it.path)
                    put("body", it.body)
                })
            }
        }.toString())
    }

    private fun read(): List<PendingAction> = runCatching {
        val array = JSONArray(secureStore.getString(KEY).orEmpty())
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            PendingAction(
                id = item.getString("id"),
                path = item.getString("path"),
                body = item.getString("body")
            )
        }
    }.getOrDefault(emptyList())

    private companion object {
        const val KEY = "pending_safety_actions"
        const val MAX_ACTIONS = 50
    }
}
