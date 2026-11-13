package com.opencode.mobile

import kotlinx.coroutines.Dispatchers
import android.net.Uri
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class SessionItem(
    val id: String,
    val title: String,
    val updated: Long,
    val created: Long,
    val cost: Double,
    val running: Boolean = false
)

sealed interface Part {
    data class Text(val text: String) : Part
    data class Image(val mime: String, val base64: String) : Part
    data class Reasoning(val text: String, val durationMs: Long) : Part
    data class Tool(
        val name: String,
        val status: String,
        val summary: String,
        val input: String,
        val output: String,
        val error: String
    ) : Part
}

data class Msg(
    val id: String,
    val role: String,
    val parts: List<Part>,
    val pending: Boolean,
    val created: Long = 0L
) {
    val plainText: String
        get() = parts.filterIsInstance<Part.Text>().joinToString("\n\n") { it.text }

    val hasVisibleContent: Boolean
        get() = parts.any {
            it is Part.Tool || it is Part.Image || it is Part.Reasoning ||
                (it is Part.Text && it.text.isNotBlank())
        }
}

data class Attachment(
    val name: String,
    val mime: String,
    val base64: String
)

data class PendingPermission(
    val id: String,
    val sessionID: String,
    val action: String,
    val detail: String
)

/**
 * Minimal client for the OpenCode V2 HTTP API.
 *
 * Auth is HTTP Basic with the fixed user "opencode" and the server password.
 */
class Api(server: String, private val password: String) {

    private val root = server.trim().trimEnd('/')
    private val jsonType = "application/json".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val streamClient = client.newBuilder()
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    private fun request(path: String): Request.Builder =
        Request.Builder()
            .url(root + path)
            .header("Authorization", Credentials.basic("opencode", password))

    private fun getJson(path: String): JSONObject {
        client.newCall(request(path).get().build()).execute().use { r ->
            val body = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw IOException("HTTP ${r.code} ${body.take(200)}")
            return JSONObject(body)
        }
    }

    private fun postJson(path: String, payload: String): JSONObject {
        val req = request(path).post(payload.toRequestBody(jsonType)).build()
        client.newCall(req).execute().use { r ->
            val body = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw IOException("HTTP ${r.code} ${body.take(200)}")
            return JSONObject(body)
        }
    }

    suspend fun ping(): String = withContext(Dispatchers.IO) {
        getJson("/api/info").optString("version")
    }

    suspend fun sessions(): List<SessionItem> = withContext(Dispatchers.IO) {
        val active = runCatching { activeSessions() }.getOrDefault(emptySet())
        val data = getJson("/api/session").optJSONArray("data") ?: JSONArray()
        (0 until data.length()).map { i ->
            val o = data.getJSONObject(i)
            val time = o.optJSONObject("time")
            val id = o.getString("id")
            SessionItem(
                id = id,
                title = o.optString("title").ifBlank { "Untitled session" },
                updated = time?.optLong("updated") ?: 0L,
                created = time?.optLong("created") ?: 0L,
                cost = o.optDouble("cost", 0.0),
                running = id in active
            )
        }.sortedByDescending { it.updated }
    }

    suspend fun createSession(): String = withContext(Dispatchers.IO) {
        postJson("/api/session", "{}").getJSONObject("data").getString("id")
    }

    /** IDs of sessions the server reports as currently running. */
    suspend fun activeSessions(): Set<String> = withContext(Dispatchers.IO) {
        val data = getJson("/api/session/active").optJSONObject("data") ?: JSONObject()
        data.keys().asSequence().toSet()
    }

    data class MessagePage(val messages: List<Msg>, val olderCursor: String?)

    suspend fun messages(
        sessionID: String,
        limit: Int = 50,
        olderCursor: String? = null
    ): MessagePage = withContext(Dispatchers.IO) {
        val query = buildString {
            append("/api/session/").append(sessionID).append("/message?limit=").append(limit)
            append("&order=desc")
            if (olderCursor != null) append("&cursor=").append(Uri.encode(olderCursor))
        }
        val page = getJson(query)
        val data = page.optJSONArray("data") ?: JSONArray()
        val messages = (0 until data.length())
            .mapNotNull { i -> parseMessage(data.getJSONObject(i)) }
            .reversed()
        val next = page.optJSONObject("cursor")?.optString("next")
        MessagePage(messages, next?.takeIf { it.isNotBlank() })
    }

    private fun parseMessage(o: JSONObject): Msg? {
        val id = o.optString("id")
        return when (o.optString("type")) {
            "user" -> {
                val parts = mutableListOf<Part>()
                val files = o.optJSONArray("files")
                if (files != null) {
                    for (i in 0 until files.length()) {
                        val file = files.optJSONObject(i) ?: continue
                        val data = file.optString("data")
                        val mime = file.optString("mime")
                        if (data.isNotBlank() && mime.startsWith("image/")) {
                            parts += Part.Image(mime, data)
                        }
                    }
                }
                val text = o.optString("text")
                if (text.isNotBlank()) parts += Part.Text(text)
                Msg(
                    id = id,
                    role = "user",
                    parts = parts,
                    pending = false,
                    created = o.optJSONObject("time")?.optLong("created") ?: 0L
                )
            }

            "assistant" -> {
                val parts = mutableListOf<Part>()
                val content = o.optJSONArray("content") ?: JSONArray()
                for (i in 0 until content.length()) {
                    val part = content.getJSONObject(i)
                    when (part.optString("type")) {
                        "text" -> {
                            val text = part.optString("text")
                            if (text.isNotBlank()) parts += Part.Text(text)
                        }

                        "reasoning" -> {
                            val text = part.optString("text")
                            if (text.isNotBlank()) {
                                val time = part.optJSONObject("time")
                                val created = time?.optLong("created") ?: 0L
                                val completed = time?.optLong("completed") ?: 0L
                                parts += Part.Reasoning(
                                    text = text,
                                    durationMs = if (completed > created && created > 0) {
                                        completed - created
                                    } else {
                                        0L
                                    }
                                )
                            }
                        }

                        "tool" -> {
                            val state = part.optJSONObject("state")
                            val inputObj = state?.optJSONObject("input")
                            val content = state?.optJSONArray("content")
                            val output = buildString {
                                if (content != null) {
                                    for (i in 0 until content.length()) {
                                        val item = content.optJSONObject(i) ?: continue
                                        when (item.optString("type")) {
                                            "text" -> append(item.optString("text"))
                                            "file" -> append("[file] ")
                                                .append(
                                                    item.optString("name").ifBlank {
                                                        item.optString("uri")
                                                    }
                                                )
                                        }
                                        if (i < content.length() - 1) append('\n')
                                    }
                                }
                            }
                            parts += Part.Tool(
                                name = part.optString("name").ifBlank { "tool" },
                                status = state?.optString("status") ?: "unknown",
                                summary = toolSummary(inputObj),
                                input = inputObj?.toString(2)?.take(6000).orEmpty(),
                                output = output.take(6000),
                                error = state?.optJSONObject("error")?.optString("message").orEmpty()
                            )
                        }
                    }
                }
                val done = o.optJSONObject("time")?.has("completed") == true
                Msg(
                    id = id,
                    role = "assistant",
                    parts = parts,
                    pending = !done,
                    created = o.optJSONObject("time")?.optLong("created") ?: 0L
                )
            }

            else -> null
        }
    }

    private fun toolSummary(input: JSONObject?): String {
        if (input == null) return ""
        val keys = listOf(
            "command", "filePath", "path", "pattern", "query", "url", "description", "prompt"
        )
        for (key in keys) {
            val value = input.optString(key, "")
            if (value.isNotBlank()) return value.replace('\n', ' ').take(110)
        }
        return ""
    }

    suspend fun send(
        sessionID: String,
        text: String,
        files: List<Attachment> = emptyList(),
        delivery: String? = null
    ): Unit = withContext(Dispatchers.IO) {
        val body = JSONObject().put("text", text)
        if (files.isNotEmpty()) {
            val arr = JSONArray()
            files.forEach { file ->
                arr.put(
                    JSONObject()
                        .put("uri", "data:${file.mime};base64,${file.base64}")
                        .put("source", JSONObject().put("type", "inline"))
                        .put("name", file.name)
                )
            }
            body.put("files", arr)
        }
        if (delivery != null) body.put("delivery", delivery)
        postJson("/api/session/$sessionID/prompt", body.toString())
    }

    suspend fun interrupt(sessionID: String): Unit = withContext(Dispatchers.IO) {
        postJson("/api/session/$sessionID/interrupt", "{}")
        Unit
    }

    suspend fun permissions(): List<PendingPermission> = withContext(Dispatchers.IO) {
        val data = getJson("/api/permission/request").optJSONArray("data") ?: JSONArray()
        (0 until data.length()).map { i ->
            val o = data.getJSONObject(i)
            val resources = o.optJSONArray("resources")
            val res = (0 until (resources?.length() ?: 0))
                .joinToString("\n") { resources!!.optString(it) }
            PendingPermission(
                id = o.getString("id"),
                sessionID = o.getString("sessionID"),
                action = o.optString("action"),
                detail = listOf(o.optString("message"), res)
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
            )
        }
    }

    suspend fun reply(sessionID: String, requestID: String, decision: String): Unit =
        withContext(Dispatchers.IO) {
            postJson(
                "/api/session/$sessionID/permission/$requestID/reply",
                JSONObject().put("decision", decision).toString()
            )
            Unit
        }

    /** Emits once for every server-sent event; used as a "something changed" signal. */
    fun events(): Flow<Unit> = callbackFlow {
        val call = streamClient.newCall(
            request("/api/event").header("Accept", "text/event-stream").get().build()
        )
        val thread = Thread {
            try {
                call.execute().use { r ->
                    if (!r.isSuccessful) {
                        close(IOException("event stream HTTP ${r.code}"))
                        return@use
                    }
                    val source = r.body?.source()
                    if (source == null) {
                        close(IOException("event stream had no body"))
                        return@use
                    }
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (line.startsWith("data:")) trySend(Unit)
                    }
                }
                close()
            } catch (t: Throwable) {
                close(t)
            }
        }
        thread.isDaemon = true
        thread.start()
        awaitClose { call.cancel() }
    }
}
