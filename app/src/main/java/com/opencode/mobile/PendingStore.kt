package com.opencode.mobile

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class PendingAttachment(
    val name: String,
    val mime: String,
    val path: String
)

/** Prompts written while the server was unreachable, delivered by the watcher. */
data class PendingPrompt(
    val id: String,
    val sessionID: String,
    val title: String,
    val text: String,
    val skillIDs: List<String>,
    val attachments: List<PendingAttachment> = emptyList()
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
                val attachmentsJson = o.optJSONArray("attachments") ?: JSONArray()
                PendingPrompt(
                    id = o.optString("id"),
                    sessionID = o.optString("sessionID"),
                    title = o.optString("title"),
                    text = o.optString("text"),
                    skillIDs = (0 until skills.length()).map { skills.optString(it) },
                    attachments = (0 until attachmentsJson.length()).mapNotNull { index ->
                        val att = attachmentsJson.optJSONObject(index) ?: return@mapNotNull null
                        PendingAttachment(
                            name = att.optString("name"),
                            mime = att.optString("mime"),
                            path = att.optString("path")
                        )
                    }
                )
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun add(
        context: Context,
        sessionID: String,
        title: String,
        text: String,
        skillIDs: List<String>,
        attachments: List<PendingAttachment> = emptyList()
    ) {
        val next = all(context) + PendingPrompt(
            id = "${System.currentTimeMillis()}-${(0..999).random()}",
            sessionID = sessionID,
            title = title,
            text = text,
            skillIDs = skillIDs,
            attachments = attachments
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
            val attachments = JSONArray()
            p.attachments.forEach { att ->
                attachments.put(
                    JSONObject()
                        .put("name", att.name)
                        .put("mime", att.mime)
                        .put("path", att.path)
                )
            }
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("sessionID", p.sessionID)
                    .put("title", p.title)
                    .put("text", p.text)
                    .put("skills", skills)
                    .put("attachments", attachments)
            )
        }
        SecurePrefs.get(context).edit().putString(KEY, arr.toString()).apply()
    }
}
