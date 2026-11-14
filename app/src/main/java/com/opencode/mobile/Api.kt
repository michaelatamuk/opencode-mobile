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
    val running: Boolean = false,
    val busyAt: Long = 0L,
    val modelID: String = "",
    val modelProviderID: String = "",
    val agentID: String = "",
    val projectID: String = "",
    val directory: String = "",
    val tokensInput: Long = 0L,
    val tokensOutput: Long = 0L
)

data class ProjectOption(val id: String, val name: String)

data class AgentOption(val id: String, val name: String, val description: String)

data class ModelOption(
    val id: String,
    val providerID: String,
    val name: String,
    val family: String,
    val contextLimit: Long = 0L
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
    val created: Long = 0L,
    val tokensInput: Long = 0L,
    val tokensCacheRead: Long = 0L
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

data class FormOption(val value: String, val label: String)

data class FormField(
    val key: String,
    val title: String,
    val description: String,
    val type: String,
    val required: Boolean,
    val options: List<FormOption>,
    val default: Any?
)

data class FormRequest(
    val id: String,
    val sessionID: String,
    val title: String,
    val fields: List<FormField>
)

data class QueuedMessage(
    val id: String,
    val text: String,
    val delivery: String
)

data class FileDiff(
    val file: String,
    val patch: String,
    val additions: Int,
    val deletions: Int,
    val status: String
)

data class CommandOption(val name: String, val description: String)

data class SkillOption(
    val id: String,
    val name: String,
    val description: String
)

/**
 * Minimal client for the OpenCode V2 HTTP API.
 *
 * Auth is HTTP Basic with the fixed user "opencode" and the server password.
 */
/** HTTP-level failure from the OpenCode server. */
class ApiException(val status: Int, body: String) :
    Exception(if (body.isBlank()) "HTTP $status" else "HTTP $status: $body")

private fun JSONObject.toSession(): SessionItem {
    val time = optJSONObject("time")
    val model = optJSONObject("model")
    return SessionItem(
        id = getString("id"),
        title = optString("title").ifBlank { "Untitled session" },
        updated = time?.optLong("updated") ?: 0L,
        created = time?.optLong("created") ?: 0L,
        cost = optDouble("cost", 0.0),
        busyAt = time?.optLong("busy") ?: 0L,
        modelID = model?.optString("id").orEmpty(),
        modelProviderID = model?.optString("providerID").orEmpty(),
        agentID = optString("agent"),
        projectID = optString("projectID"),
        directory = optJSONObject("location")?.optString("directory").orEmpty(),
        tokensInput = optJSONObject("tokens")?.optLong("input") ?: 0L,
        tokensOutput = optJSONObject("tokens")?.optLong("output") ?: 0L
    )
}

/** Merge a freshly fetched page into already loaded history, newest wins. */
internal fun mergeMessages(existing: List<Msg>, incoming: List<Msg>): List<Msg> {
    if (incoming.isEmpty()) return existing
    val byId = LinkedHashMap<String, Msg>(existing.size + incoming.size)
    existing.forEach { byId[it.id] = it }
    incoming.forEach { byId[it.id] = it }
    return byId.values.sortedBy { it.created }
}

/** Redeems an OpenCode pairing code without a camera: paste the code or link. */
object Pairing {

    /**
     * Accepts either a bare code or the full `.../auth/connect/<code>` link.
     * Returns the token to store as the server password.
     */
    suspend fun redeem(server: String, codeOrLink: String): String =
        withContext(Dispatchers.IO) {
            val trimmed = codeOrLink.trim()
            val linkIndex = trimmed.indexOf("/auth/connect/")
            val (base, code) = if (linkIndex >= 0) {
                val prefix = trimmed.substring(0, linkIndex)
                val code = trimmed.substring(linkIndex + "/auth/connect/".length)
                    .substringBefore('?')
                    .substringBefore('#')
                prefix to code
            } else {
                server.trim().trimEnd('/') to trimmed.substringAfterLast('/')
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
            client.newCall(
                Request.Builder()
                    .url("$base/auth/connect/${Uri.encode(code)}")
                    .header("Accept", "application/json")
                    .get()
                    .build()
            ).execute().use { r ->
                val body = r.body?.string().orEmpty()
                if (!r.isSuccessful) throw ApiException(r.code, body.take(300))
                JSONObject(body).optString("token")
                    .ifBlank { throw ApiException(500, "No token in pairing response") }
            }
        }
}

/** A user-facing message for any failure from the API. */
fun friendlyError(error: Throwable): String = when (error) {
    is ApiException -> when (error.status) {
        401, 403 -> "Wrong or missing server password"
        404 -> "Not found on this server"
        in 500..599 -> "Server error (${error.status})"
        else -> error.message ?: "Request failed"
    }

    is java.net.UnknownHostException -> "Server address not found"
    is java.net.ConnectException -> "Cannot connect — is the server running?"
    is java.net.SocketTimeoutException -> "Server took too long to answer"
    else -> error.message ?: "Something went wrong"
}

class Api(server: String, private val password: String, fallbackServer: String? = null) {

    private val primaryRoot = server.trim().trimEnd('/')
    private val fallbackRoot = fallbackServer
        ?.trim()
        ?.trimEnd('/')
        ?.takeIf { it.isNotBlank() && it != primaryRoot }

    /** The address currently in use; switches to the fallback after a transport failure. */
    @Volatile
    private var activeRoot = primaryRoot

    private val root: String get() = activeRoot
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

    private fun getJson(path: String): JSONObject = withFallback {
        client.newCall(request(path).get().build()).execute().use { r ->
            val body = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw ApiException(r.code, body.take(300))
            JSONObject(body)
        }
    }

    /**
     * Runs a request, and once a transport failure hits the primary address,
     * retries it against the fallback (and stays there while it works).
     */
    private fun <T> withFallback(block: () -> T): T {
        return try {
            block()
        } catch (e: IOException) {
            val fallback = fallbackRoot ?: throw e
            if (activeRoot == fallback) throw e
            activeRoot = fallback
            try {
                block()
            } catch (second: IOException) {
                activeRoot = primaryRoot
                throw second
            }
        }
    }

    private fun postJson(path: String, payload: String): JSONObject = withFallback {
        val req = request(path).post(payload.toRequestBody(jsonType)).build()
        client.newCall(req).execute().use { r ->
            val body = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw ApiException(r.code, body.take(300))
            JSONObject(body)
        }
    }

    /** POST for endpoints that answer 204 and therefore have no JSON body. */
    private fun postEmpty(path: String, payload: String) = withFallback {
        val req = request(path).post(payload.toRequestBody(jsonType)).build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw ApiException(r.code, r.body?.string().orEmpty().take(300))
        }
    }

    private fun deleteEmpty(path: String) = withFallback {
        client.newCall(request(path).delete().build()).execute().use { r ->
            if (!r.isSuccessful) throw ApiException(r.code, r.body?.string().orEmpty().take(300))
        }
    }

    suspend fun ping(): String = withContext(Dispatchers.IO) {
        getJson("/api/info").optString("version")
    }

    suspend fun sessions(): List<SessionItem> = withContext(Dispatchers.IO) {
        val active = runCatching { activeSessions() }.getOrDefault(emptySet())
        val data = getJson("/api/session").optJSONArray("data") ?: JSONArray()
        (0 until data.length())
            .map { i -> data.getJSONObject(i).toSession().copy(running = data.getJSONObject(i).getString("id") in active) }
            .sortedByDescending { it.updated }
    }

    suspend fun session(id: String): SessionItem = withContext(Dispatchers.IO) {
        getJson("/api/session/$id").getJSONObject("data").toSession()
    }

    /** One-line preview of the newest real message, for the session list. */
    suspend fun lastPreview(sessionID: String): String? {
        val page = messages(sessionID, limit = 8)
        val raw = page.messages.asReversed()
            .firstOrNull { it.plainText.isNotBlank() }
            ?.plainText
            .orEmpty()
        // Strip markdown markers so the list shows prose, not syntax.
        val cleaned = raw
            .replace(Regex("[#*_`>]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return cleaned.take(120).ifBlank { null }
    }

    suspend fun agents(): List<AgentOption> = withContext(Dispatchers.IO) {
        val data = getJson("/api/agent").optJSONArray("data") ?: JSONArray()
        (0 until data.length()).mapNotNull { i ->
            val o = data.getJSONObject(i)
            if (o.optBoolean("hidden", false)) return@mapNotNull null
            AgentOption(
                id = o.optString("id"),
                name = o.optString("name").ifBlank { o.optString("id") },
                description = o.optString("description")
            )
        }
    }

    suspend fun projects(): List<ProjectOption> = withContext(Dispatchers.IO) {
        val arr = try {
            client.newCall(request("/api/project").get().build()).execute().use { r ->
                val body = r.body?.string().orEmpty()
                if (!r.isSuccessful) throw ApiException(r.code, body.take(300))
                org.json.JSONArray(body)
            }
        } catch (e: Exception) {
            JSONArray()
        }
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ProjectOption(
                id = o.optString("id"),
                name = o.optString("name").ifBlank {
                    o.optString("canonical").substringAfterLast('\\').substringAfterLast('/')
                }
            )
        }
    }

    suspend fun models(): List<ModelOption> = withContext(Dispatchers.IO) {        val data = getJson("/api/model").optJSONArray("data") ?: JSONArray()
        (0 until data.length()).mapNotNull { i ->
            val o = data.getJSONObject(i)
            if (!o.optBoolean("enabled", true)) return@mapNotNull null
            ModelOption(
                id = o.optString("modelID").ifBlank { o.optString("id") },
                providerID = o.optString("providerID"),
                name = o.optString("name").ifBlank { o.optString("modelID") },
                family = o.optString("family"),
                contextLimit = o.optJSONObject("limit")?.optLong("context") ?: 0L
            )
        }
    }

    suspend fun setModel(sessionID: String, providerID: String, modelID: String): Unit =
        withContext(Dispatchers.IO) {
            postJson(
                "/api/session/$sessionID/model",
                JSONObject()
                    .put("model", JSONObject().put("id", modelID).put("providerID", providerID))
                    .toString()
            )
            Unit
        }

    suspend fun setAgent(sessionID: String, agentID: String): Unit = withContext(Dispatchers.IO) {
        postJson(
            "/api/session/$sessionID/agent",
            JSONObject().put("agent", agentID).toString()
        )
        Unit
    }

    suspend fun sessionDiff(sessionID: String): List<FileDiff> = withContext(Dispatchers.IO) {
        val data = getJson("/api/session/$sessionID/diff").optJSONArray("data") ?: JSONArray()
        (0 until data.length()).map { i ->
            val o = data.getJSONObject(i)
            FileDiff(
                file = o.optString("file"),
                patch = o.optString("patch"),
                additions = o.optInt("additions", 0),
                deletions = o.optInt("deletions", 0),
                status = o.optString("status")
            )
        }
    }

    suspend fun renameSession(id: String, title: String): Unit = withContext(Dispatchers.IO) {        val body = JSONObject().put("title", title).toString().toRequestBody(jsonType)
        client.newCall(request("/api/session/$id").patch(body).build()).execute().use { r ->
            if (!r.isSuccessful) throw ApiException(r.code, r.body?.string().orEmpty().take(300))
        }
    }

    suspend fun deleteSession(id: String): Unit = withContext(Dispatchers.IO) {
        client.newCall(request("/api/session/$id").delete().build()).execute().use { r ->
            if (!r.isSuccessful) throw ApiException(r.code, r.body?.string().orEmpty().take(300))
        }
    }

    suspend fun createSession(): String = withContext(Dispatchers.IO) {
        postJson("/api/session", "{}").getJSONObject("data").getString("id")
    }

    /** Creates a child session copying history before [before] (or the full history). */
    suspend fun forkSession(sessionID: String, before: String?): String = withContext(Dispatchers.IO) {
        val body = JSONObject().put("before", before ?: JSONObject.NULL)
        postJson("/api/session/$sessionID/fork", body.toString())
            .getJSONObject("data")
            .getString("id")
    }

    /** Stages a reversible boundary and returns the files that would be restored. */
    suspend fun stageRevert(sessionID: String, messageID: String, files: Boolean): List<String> =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("messageID", messageID).put("files", files)
            val revert = postJson("/api/session/$sessionID/revert/stage", body.toString())
                .optJSONObject("data")
                ?: return@withContext emptyList()
            val list = revert.optJSONArray("files") ?: JSONArray()
            (0 until list.length()).mapNotNull { i ->
                list.optJSONObject(i)?.optString("file")?.takeIf { it.isNotBlank() }
            }
        }

    suspend fun commitRevert(sessionID: String): Unit = withContext(Dispatchers.IO) {
        postEmpty("/api/session/$sessionID/revert/commit", "{}")
    }

    suspend fun clearRevert(sessionID: String): Unit = withContext(Dispatchers.IO) {
        deleteEmpty("/api/session/$sessionID/revert")
    }

    suspend fun compactSession(sessionID: String): Unit = withContext(Dispatchers.IO) {
        postJson("/api/session/$sessionID/compact", "{}")
        Unit
    }

    /** Reads a text file relative to [directory] (or an absolute path). */
    suspend fun readFile(path: String, directory: String): String = withContext(Dispatchers.IO) {
        val normalized = path.replace('\\', '/').removePrefix("/")
        val encodedPath = Uri.encode(normalized, "/:")
        val query = if (directory.isBlank()) {
            ""
        } else {
            "?location[directory]=" + Uri.encode(directory)
        }
        withFallback {
            client.newCall(request("/api/fs/read/$encodedPath$query").get().build()).execute().use { r ->
                val body = r.body?.string().orEmpty()
                if (!r.isSuccessful) throw ApiException(r.code, body.take(300))
                body
            }
        }
    }

    suspend fun writeFile(path: String, directory: String, content: String): Unit =
        withContext(Dispatchers.IO) {
            val query = buildString {
                append("?path=").append(Uri.encode(path))
                if (directory.isNotBlank()) {
                    append("&location[directory]=").append(Uri.encode(directory))
                }
            }
            withFallback {
                val req = request("/api/experimental/fs/write$query")
                    .post(
                        content.toByteArray()
                            .toRequestBody("application/octet-stream".toMediaType())
                    )
                    .build()
                client.newCall(req).execute().use { r ->
                    if (!r.isSuccessful) {
                        throw ApiException(r.code, r.body?.string().orEmpty().take(300))
                    }
                }
            }
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
            if (olderCursor == null) {
                append("&order=desc")
            } else {
                // The cursor carries its own order; sending `order` as well is rejected.
                append("&cursor=").append(Uri.encode(olderCursor))
            }
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
                val usage = o.optJSONObject("tokens")
                Msg(
                    id = id,
                    role = "assistant",
                    parts = parts,
                    pending = !done,
                    created = o.optJSONObject("time")?.optLong("created") ?: 0L,
                    tokensInput = usage?.optLong("input") ?: 0L,
                    tokensCacheRead = usage?.optJSONObject("cache")?.optLong("read") ?: 0L
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
        delivery: String? = null,
        skillIDs: List<String> = emptyList()
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
        if (skillIDs.isNotEmpty()) {
            val arr = JSONArray()
            skillIDs.forEach { id -> arr.put(JSONObject().put("id", id)) }
            body.put("skills", arr)
        }
        if (delivery != null) body.put("delivery", delivery)
        postJson("/api/session/$sessionID/prompt", body.toString())
    }

    suspend fun commands(): List<CommandOption> = withContext(Dispatchers.IO) {
        val data = getJson("/api/command").optJSONArray("data") ?: JSONArray()
        (0 until data.length()).map { i ->
            val o = data.getJSONObject(i)
            CommandOption(o.optString("name"), o.optString("description"))
        }
    }

    suspend fun skills(): List<SkillOption> = withContext(Dispatchers.IO) {
        val data = getJson("/api/skill").optJSONArray("data") ?: JSONArray()
        (0 until data.length()).map { i ->
            val o = data.getJSONObject(i)
            SkillOption(
                id = o.optString("id"),
                name = o.optString("name").ifBlank { o.optString("id") },
                description = o.optString("description")
            )
        }
    }

    /** Runs a slash command directly, so arguments are parsed by the server. */
    suspend fun runCommand(sessionID: String, name: String, text: String): Unit =
        withContext(Dispatchers.IO) {
            postJson(
                "/api/session/$sessionID/command",
                JSONObject().put("name", name).put("text", text).toString()
            )
            Unit
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

    suspend fun forms(sessionID: String): List<FormRequest> = withContext(Dispatchers.IO) {
        val data = getJson("/api/session/$sessionID/form").optJSONArray("data") ?: JSONArray()
        (0 until data.length()).map { index ->
            val form = data.getJSONObject(index)
            val fieldsJson = form.optJSONArray("fields") ?: JSONArray()
            val fields = (0 until fieldsJson.length()).map { i ->
                val field = fieldsJson.getJSONObject(i)
                val optionsJson = field.optJSONArray("options") ?: JSONArray()
                FormField(
                    key = field.optString("key"),
                    title = field.optString("title").ifBlank { field.optString("key") },
                    description = field.optString("description"),
                    type = field.optString("type", "string"),
                    required = field.optBoolean("required", false),
                    options = (0 until optionsJson.length()).map { o ->
                        val option = optionsJson.getJSONObject(o)
                        FormOption(option.optString("value"), option.optString("label"))
                    },
                    default = if (field.has("default") && !field.isNull("default")) {
                        field.get("default")
                    } else {
                        null
                    }
                )
            }
            FormRequest(
                id = form.getString("id"),
                sessionID = form.optString("sessionID", sessionID),
                title = form.optString("title").ifBlank { "Question" },
                fields = fields
            )
        }
    }

    suspend fun answerForm(sessionID: String, formID: String, answers: Map<String, Any>): Unit =
        withContext(Dispatchers.IO) {
            val answer = JSONObject()
            answers.forEach { (key, value) -> answer.put(key, value) }
            postJson(
                "/api/session/$sessionID/form/$formID/reply",
                JSONObject().put("answer", answer).toString()
            )
            Unit
        }

    suspend fun inbox(sessionID: String): List<QueuedMessage> = withContext(Dispatchers.IO) {
        val data = getJson("/api/session/$sessionID/inbox").optJSONArray("data") ?: JSONArray()
        (0 until data.length()).mapNotNull { index ->
            val item = data.getJSONObject(index)
            if (item.optString("type") != "user") return@mapNotNull null
            val payload = item.optJSONObject("payload")
            QueuedMessage(
                id = item.optString("id"),
                text = payload?.optString("text").orEmpty(),
                delivery = item.optString("delivery")
            )
        }
    }

    /** Emits the JSON payload of every server-sent event. */
    fun events(): Flow<String> = callbackFlow {
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
                        if (line.startsWith("data:")) trySend(line.removePrefix("data:").trim())
                    }
                }
                close()
            } catch (t: Throwable) {
                if (t is IOException) {
                    // Next reconnect should try the other address so a dead
                    // LAN/tunnel route does not wedge the watcher.
                    val fallback = fallbackRoot
                    if (fallback != null) {
                        activeRoot = if (activeRoot == primaryRoot) fallback else primaryRoot
                    }
                }
                close(t)
            }
        }
        thread.isDaemon = true
        thread.start()
        awaitClose { call.cancel() }
    }
}
