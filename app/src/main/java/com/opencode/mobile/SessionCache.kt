package com.opencode.mobile

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Last known session list, so the app still shows history when offline. */
class SessionCache(context: Context) {

    private val prefs = context.getSharedPreferences("opencode_cache", Context.MODE_PRIVATE)

    fun read(): List<SessionItem> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                SessionItem(
                    id = o.optString("id"),
                    title = o.optString("title"),
                    updated = o.optLong("updated"),
                    created = o.optLong("created"),
                    cost = o.optDouble("cost", 0.0),
                    running = false,
                    busyAt = o.optLong("busyAt"),
                    modelID = o.optString("modelID"),
                    modelProviderID = o.optString("modelProviderID"),
                    agentID = o.optString("agentID"),
                    projectID = o.optString("projectID")
                )
            }
        }.getOrDefault(emptyList())
    }

    fun write(sessions: List<SessionItem>) {
        val arr = JSONArray()
        sessions.forEach { s ->
            arr.put(
                JSONObject()
                    .put("id", s.id)
                    .put("title", s.title)
                    .put("updated", s.updated)
                    .put("created", s.created)
                    .put("cost", s.cost)
                    .put("busyAt", s.busyAt)
                    .put("modelID", s.modelID)
                    .put("modelProviderID", s.modelProviderID)
                    .put("agentID", s.agentID)
                    .put("projectID", s.projectID)
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private companion object {
        const val KEY = "sessions"
    }
}
