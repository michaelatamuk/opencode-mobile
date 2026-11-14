package com.opencode.mobile

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Prompts written while the server was unreachable, delivered by the watcher. */
data class PendingPrompt(
    val id: String,
    val sessionID: String,
    val text: String,
    val skillIDs: List<String>
)

object PendingStore {

    private const val KEY = "pending_prompts"

    @Synchronized
    fun all(context: Context): List<PendingPrompt> {
        val raw = SecurePrefs.get(context).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val skills = o.optJSONArray("skills") ?: JSONArray()
                PendingPrompt(
                    id = o.optString("id"),
                    sessionID = o.optString("sessionID"),
                    text = o.optString("text"),
                    skillIDs = (0 until skills.length()).map { skills.optString(it) }
                )
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun add(context: Context, sessionID: String, text: String, skillIDs: List<String>) {
        val next = all(context) + PendingPrompt(
            id = "${System.currentTimeMillis()}-${(0..999).random()}",
            sessionID = sessionID,
            text = text,
            skillIDs = skillIDs
        )
        write(context, next.takeLast(20))
    }

    @Synchronized
    fun remove(context: Context, id: String) {
        write(context, all(context).filterNot { it.id == id })
    }

    private fun write(context: Context, items: List<PendingPrompt>) {
        val arr = JSONArray()
        items.forEach { p ->
            val skills = JSONArray()
            p.skillIDs.forEach { skills.put(it) }
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("sessionID", p.sessionID)
                    .put("text", p.text)
                    .put("skills", skills)
            )
        }
        SecurePrefs.get(context).edit().putString(KEY, arr.toString()).apply()
    }
}
