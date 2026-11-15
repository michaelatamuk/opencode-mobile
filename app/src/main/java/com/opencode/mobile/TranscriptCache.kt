package com.opencode.mobile

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Last-known transcript per session, so opening a session while the server is
 * unreachable still shows what happened. Images are skipped: base64 payloads
 * would dwarf the text and are the least useful offline.
 */
object TranscriptCache {

    private const val LIMIT = 60
    private const val VERSION = 1

    private fun dir(context: Context): File =
        File(context.filesDir, "transcripts").apply { mkdirs() }

    private fun file(context: Context, sessionID: String): File =
        File(dir(context), "${sessionID.replace('/', '_')}.json")

    fun save(context: Context, sessionID: String, messages: List<Msg>) {
        if (sessionID.isBlank() || messages.isEmpty()) return
        runCatching {
            val tail = messages.takeLast(LIMIT)
            val root = JSONObject().apply {
                put("v", VERSION)
                put("messages", JSONArray().apply {
                    tail.forEach { put(it.toJson()) }
                })
            }
            file(context, sessionID).writeText(root.toString())
        }
    }

    fun load(context: Context, sessionID: String): List<Msg> {
        if (sessionID.isBlank()) return emptyList()
        return runCatching {
            val root = JSONObject(file(context, sessionID).readText())
            if (root.optInt("v") != VERSION) return emptyList()
            val arr = root.optJSONArray("messages") ?: return emptyList()
            (0 until arr.length()).mapNotNull { index ->
                arr.optJSONObject(index)?.toMsg()
            }
        }.getOrDefault(emptyList())
    }

    private fun Msg.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("role", role)
        put("pending", pending)
        put("created", created)
        put("completed", completed)
        put("tokensInput", tokensInput)
        put("tokensCacheRead", tokensCacheRead)
        put("parts", JSONArray().apply {
            parts.forEach { part ->
                when (part) {
                    is Part.Text -> put(
                        JSONObject().put("t", "text").put("text", part.text)
                    )

                    is Part.Reasoning -> put(
                        JSONObject().put("t", "reasoning")
                            .put("text", part.text)
                            .put("durationMs", part.durationMs)
                    )

                    is Part.Tool -> put(
                        JSONObject().put("t", "tool")
                            .put("name", part.name)
                            .put("status", part.status)
                            .put("summary", part.summary)
                            .put("input", part.input)
                            .put("output", part.output)
                            .put("error", part.error)
                    )

                    is Part.Image -> Unit
                }
            }
        })
    }

    private fun JSONObject.toMsg(): Msg? {
        val id = optString("id").ifBlank { return null }
        val role = optString("role").ifBlank { "assistant" }
        val partsArr = optJSONArray("parts") ?: JSONArray()
        val parts = (0 until partsArr.length()).mapNotNull { index ->
            partsArr.optJSONObject(index)?.toPart()
        }
        return Msg(
            id = id,
            role = role,
            parts = parts,
            pending = optBoolean("pending"),
            created = optLong("created"),
            completed = optLong("completed"),
            tokensInput = optLong("tokensInput"),
            tokensCacheRead = optLong("tokensCacheRead")
        )
    }

    private fun JSONObject.toPart(): Part? = when (optString("t")) {
        "text" -> Part.Text(optString("text"))
        "reasoning" -> Part.Reasoning(optString("text"), optLong("durationMs"))
        "tool" -> Part.Tool(
            name = optString("name"),
            status = optString("status"),
            summary = optString("summary"),
            input = optString("input"),
            output = optString("output"),
            error = optString("error")
        )

        else -> null
    }
}
