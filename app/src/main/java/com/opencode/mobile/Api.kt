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
import okhttp3.RequestBody
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
    val parentID: String = "",
    val forkedFrom: String = "",
    val archived: Boolean = false,
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
    val contextLimit: Long = 0L,
    val providerName: String = ""
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
    val completed: Long = 0L,
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

data class ScheduleItem(
    val id: String,
    val prompt: String,
    val status: String,
    val nextDueAt: Long,
    val runCount: Int,
    val lastOutput: String = "",
    /** "in", "at", "every", or "cron"; blank when the server omits it. */
    val whenType: String = "",
    /** The raw value of [whenType], e.g. "1 hour" or "0 9 * * 1-5". */
    val whenValue: String = "",
    /** Runs left for a schedule with a stop limit; 0 when unlimited. */
    val remainingRuns: Int = 0,
    /** Shell command that must succeed before the wake, when set. */
    val condition: String = ""
)

data class McpServer(val name: String, val status: String)

data class McpResource(
    val server: String,
    val name: String,
    val uri: String,
    val description: String = ""
)

data class IntegrationItem(
    val id: String,
    val name: String,
    val connected: Boolean,
    val methods: List<String>,
    val envNames: List<String>
)

data class ShellRun(val id: String, val status: String)

/** One command currently running in a workspace directory. */
data class RunningShell(
    val id: String,
    val command: String,
    val status: String,
    val startedAt: Long,
    val sessionID: String = ""
)

/** A background job owned by a session: shell, Code Mode, or subagent work. */
data class JobItem(
    val id: String,
    val kind: String,
    val title: String,
    val status: String,
    val sessionID: String,
    val startedAt: Long
)

data class ContextEntry(val type: String, val text: String, val created: Long)

data class ContextSummary(
    val total: Int,
    val counts: Map<String, Int>,
    val inputTokens: Long,
    val outputTokens: Long,
    val cost: Double,
    val compactionSummary: String,
    val compactionRecent: String,
    val recent: List<ContextEntry>
)

data class SavedPermission(
    val id: String,
    val action: String,
    val resource: String,
    val effect: String
)

data class InstructionEntry(val key: String, val value: String)

data class VcsChange(
    val path: String,
    val status: String,
    val additions: Int,
    val deletions: Int
)

data class WorktreeEntry(val directory: String, val strategy: String)

data class VcsInfo(val provider: String, val branch: String, val defaultBranch: String)

data class SkillOption(
    val id: String,
    val name: String,
    val description: String,
    val content: String = ""
)

/**
 * Minimal client for the OpenCode V2 HTTP API.
 *
 * Auth is HTTP Basic with the fixed user "opencode" and the server password.
 */
/** HTTP-level failure from the OpenCode server. */
class ApiException(val status: Int, body: String) :
    Exception(if (body.isBlank()) "HTTP $status" else "HTTP $status: $body")

/**
 * Schedules exist on this server but are turned off. The server answers 403
 * for a disabled `experimental.schedules`, which is easy to mistake for a
 * wrong password, so it gets its own error.
 */
class SchedulesDisabledException : Exception(
    "Schedules are turned off on this server. Add the flag below to its config, then check again."
)

/** The server predates the schedule endpoints. */
class SchedulesUnsupportedException : Exception(
    "This server version has no schedules. Update OpenCode on the computer."
)

/** 2.4+ calls them automations; the workspace can turn them off. */
class AutomationsDisabledException : Exception(
    "Schedules are disabled for this workspace. Set \"automations\": true in the server config, then check again."
)

/** 403/404 on schedule routes mean a missing feature, not bad credentials. */
internal fun scheduleFailure(error: ApiException): Exception = when (error.status) {
    403 -> SchedulesDisabledException()
    404 -> SchedulesUnsupportedException()
    else -> error
}

/** Condenses an assistant context item into one line of prose and tool names. */
private fun assistantContextText(o: JSONObject): String {
    val content = o.optJSONArray("content") ?: return o.optString("text")
    val parts = mutableListOf<String>()
    for (i in 0 until content.length()) {
        val p = content.optJSONObject(i) ?: continue
        when (p.optString("type")) {
            "text" -> p.optString("text").takeIf { it.isNotBlank() }?.let(parts::add)
            "tool" -> p.optString("name")
                .ifBlank { p.optString("tool") }
                .takeIf { it.isNotBlank() }
                ?.let { parts.add("[$it]") }
        }
    }
    return parts.joinToString(" ")
}

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
        parentID = optString("parentID"),
        forkedFrom = optJSONObject("fork")?.optString("sessionID").orEmpty(),
        archived = (time?.optLong("archived") ?: 0L) > 0L,
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

    data class Result(val token: String, val server: String)

    /**
     * Accepts a bare code or a link. A complete `http(s)` link is opened exactly
     * as printed by the server (re-encoding its code is how "expired" errors
     * happen); app deep links have their code/url extracted; loopback links from
     * the server machine are rewritten to the configured server.
     */
    suspend fun redeem(server: String, codeOrLink: String): Result =
        withContext(Dispatchers.IO) {
            val raw = codeOrLink.trim()
            val configured = server.trim().trimEnd('/')
            val uri = runCatching { Uri.parse(raw) }.getOrNull()
            val scheme = uri?.scheme?.lowercase().orEmpty()
            var explicitServer: String? = null

            // The desktop app's QR is JSON: {"code":"…","url":"http://…"}.
            val payload = if (raw.startsWith("{")) {
                runCatching { JSONObject(raw) }.getOrNull()
                    ?: throw ApiException(400, "Unreadable pairing QR")
            } else {
                null
            }

            val target = when {
                payload != null -> {
                    val link = payload.optString("link").trim()
                    val code = payload.optString("code").trim()
                        .ifBlank { payload.optString("token").trim() }
                    // The desktop QR is {"code":"…","urls":["https://…tunnel…"]}.
                    val arrayUrl = payload.optJSONArray("urls")?.let { arr ->
                        val all = (0 until arr.length())
                            .map { arr.optString(it).trim() }
                            .filter { it.startsWith("http", true) }
                        all.firstOrNull { it.startsWith("https", true) } ?: all.firstOrNull()
                    }
                    explicitServer = arrayUrl?.trimEnd('/')
                        ?: listOf(
                            "url", "serverUrl", "server_url", "server",
                            "base", "origin", "host", "endpoint", "address"
                        )
                            .map { payload.optString(it).trim() }
                            .firstOrNull { it.isNotBlank() }
                            ?.trimEnd('/')
                        ?: payload.optJSONObject("server")
                            ?.optString("url")
                            ?.trim()
                            ?.trimEnd('/')
                    when {
                        link.startsWith("http", true) -> rewriteLoopback(link, configured)
                        code.isNotBlank() -> {
                            val base = explicitServer ?: configured
                            if (base.isBlank()) throw ApiException(400, "no_server")
                            "${base.trimEnd('/')}/auth/connect/${Uri.encode(code)}"
                        }
                        else -> throw ApiException(400, "No pairing code in this QR")
                    }
                }

                scheme == "http" || scheme == "https" -> rewriteLoopback(raw, configured)

                scheme.isNotEmpty() && scheme != "file" -> {
                    val nested = uri?.getQueryParameter("url")
                        ?: uri?.getQueryParameter("server")
                        ?: configured
                    val code = uri?.getQueryParameter("code")
                        ?: uri?.getQueryParameter("token")
                        ?: uri?.lastPathSegment
                        ?: throw ApiException(400, "No pairing code in this link")
                    "${nested.trim().trimEnd('/')}/auth/connect/${Uri.encode(Uri.decode(code))}"
                }

                else -> {
                    val code = raw.substringAfterLast('/')
                    if (configured.isBlank()) throw ApiException(400, "no_server")
                    "$configured/auth/connect/${Uri.encode(Uri.decode(code))}"
                }
            }
            if (!target.startsWith("http://", true) && !target.startsWith("https://", true)) {
                throw ApiException(400, "no_server")
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
            client.newCall(
                Request.Builder()
                    .url(target)
                    .header("Accept", "application/json")
                    .get()
                    .build()
            ).execute().use { r ->
                val body = r.body?.string().orEmpty()
                if (!r.isSuccessful) throw ApiException(r.code, body.take(300))
                val token = JSONObject(body).optString("token")
                    .ifBlank { throw ApiException(500, "No token in pairing response") }
                val usedServer = target.substringBefore("/auth/connect/")
                Result(token = token, server = usedServer.ifBlank { configured })
            }
        }

    /** The server prints its own loopback links too; use the phone's server instead. */
    private fun rewriteLoopback(url: String, configured: String): String {
        val link = runCatching { Uri.parse(url) }.getOrNull() ?: return url
        val host = link.host ?: return url
        if (host != "127.0.0.1" && host != "localhost" && host != "::1" && host != "0.0.0.0") {
            return url
        }
        val base = runCatching { Uri.parse(configured) }.getOrNull() ?: return url
        if (base.host.isNullOrBlank()) return url
        return link.buildUpon()
            .scheme(base.scheme ?: link.scheme)
            .authority(base.authority)
            .build()
            .toString()
    }
}

/** A user-facing message for any failure from the API. */
fun friendlyError(error: Throwable): String = when (error) {
    is SchedulesDisabledException, is SchedulesUnsupportedException,
    is AutomationsDisabledException ->
        error.message ?: "Schedules are unavailable on this server"

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

    private fun patchEmpty(path: String, payload: String) = withFallback {
        val req = request(path).patch(payload.toRequestBody(jsonType)).build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw ApiException(r.code, r.body?.string().orEmpty().take(300))
        }
    }

    private fun putEmpty(path: String, payload: String) = withFallback {
        val req = request(path).put(payload.toRequestBody(jsonType)).build()
        client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw ApiException(r.code, r.body?.string().orEmpty().take(300))
        }
    }

    private fun deleteWithBody(path: String, payload: String) = withFallback {
        val req = request(path).delete(payload.toRequestBody(jsonType)).build()
        client.newCall(req).execute().use { r ->
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

    suspend fun models(): List<ModelOption> = withContext(Dispatchers.IO) {
        // Only providers explicitly enabled on this server ("defined on the
        // computer"), not every model from the public catalog.
        val providerNames = runCatching {
            val providers = getJson("/api/provider").optJSONArray("data") ?: JSONArray()
            (0 until providers.length()).mapNotNull { index ->
                val o = providers.optJSONObject(index) ?: return@mapNotNull null
                if (o.optString("activation") != "enabled") return@mapNotNull null
                val id = o.optString("id")
                if (id.isBlank()) return@mapNotNull null
                id to o.optString("name").ifBlank { id }
            }.toMap()
        }.getOrDefault(emptyMap())

        val data = getJson("/api/model").optJSONArray("data") ?: JSONArray()
        (0 until data.length()).mapNotNull { i ->
            val o = data.getJSONObject(i)
            if (!o.optBoolean("enabled", true)) return@mapNotNull null
            val providerID = o.optString("providerID")
            if (providerNames.isNotEmpty() && providerID !in providerNames) {
                return@mapNotNull null
            }
            ModelOption(
                id = o.optString("modelID").ifBlank { o.optString("id") },
                providerID = providerID,
                name = o.optString("name").ifBlank { o.optString("modelID") },
                family = o.optString("family"),
                contextLimit = o.optJSONObject("limit")?.optLong("context") ?: 0L,
                providerName = providerNames[providerID].orEmpty()
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

    suspend fun setArchived(id: String, archived: Boolean): Unit = withContext(Dispatchers.IO) {
        patchEmpty(
            "/api/session/$id",
            JSONObject().put("archived", archived).toString()
        )
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
                    completed = o.optJSONObject("time")?.optLong("completed") ?: 0L,
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
        skillIDs: List<String> = emptyList(),
        onProgress: ((Float) -> Unit)? = null
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
        val payload = body.toString()
        val payloadBytes = payload.toByteArray()
        val requestBody: RequestBody = if (onProgress == null) {
            payload.toRequestBody(jsonType)
        } else {
            object : RequestBody() {
                override fun contentType() = jsonType
                override fun contentLength() = payloadBytes.size.toLong()
                override fun writeTo(sink: okio.BufferedSink) {
                    var offset = 0
                    while (offset < payloadBytes.size) {
                        val count = minOf(8192, payloadBytes.size - offset)
                        sink.write(payloadBytes, offset, count)
                        offset += count
                        onProgress(offset.toFloat() / payloadBytes.size)
                    }
                }
            }
        }
        val req = request("/api/session/$sessionID/prompt").post(requestBody).build()
        client.newCall(req).execute().use { r ->
            val responseBody = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw ApiException(r.code, responseBody.take(300))
        }
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
                description = o.optString("description"),
                content = o.optString("content")
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

    /** Detaches the session's active foreground tools and lets the turn continue. */
    suspend fun background(sessionID: String): Unit = withContext(Dispatchers.IO) {
        postEmpty("/api/session/$sessionID/background", "{}")
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

    /** Cancels a queued inbox item that has not been delivered yet. */
    suspend fun cancelInbox(sessionID: String, inboxID: String): Unit =
        withContext(Dispatchers.IO) {
            deleteEmpty("/api/session/$sessionID/inbox/$inboxID")
        }

    /** Moves a queued item between queue and steer delivery. */
    suspend fun updateInbox(sessionID: String, inboxID: String, delivery: String): Unit =
        withContext(Dispatchers.IO) {
            patchEmpty(
                "/api/session/$sessionID/inbox/$inboxID",
                JSONObject().put("delivery", delivery).toString()
            )
        }

    private fun parseScheduleItems(payload: JSONObject, whenKey: String): List<ScheduleItem> {
        val data = payload.optJSONArray("data") ?: JSONArray()
        return (0 until data.length()).map { i ->
            val o = data.getJSONObject(i)
            val raw = o.opt("status")
            val status = when (raw) {
                is String -> raw
                is JSONObject -> raw.optString("type").ifBlank { raw.optString("status") }
                else -> ""
            }
            val whenObj = o.optJSONObject(whenKey)
            val whenType = whenObj?.optString("type").orEmpty()
            val action = o.optJSONObject("action")
            ScheduleItem(
                id = o.optString("id"),
                prompt = action?.optString("prompt").orEmpty()
                    .ifBlank { o.optString("name") },
                status = status,
                nextDueAt = o.optLong("nextDueAt"),
                runCount = o.optInt("runCount"),
                lastOutput = o.optString("lastOutput"),
                whenType = whenType,
                whenValue = when (whenType) {
                    "in" -> whenObj?.optString("in").orEmpty()
                    "at" -> whenObj?.optString("at").orEmpty()
                    "every" -> whenObj?.optString("every").orEmpty()
                    "cron" -> whenObj?.optString("cron").orEmpty()
                    else -> ""
                },
                remainingRuns = o.optInt("remainingRuns"),
                condition = o.optJSONObject("if")?.optString("run").orEmpty()
            )
        }
    }

    /** OpenCode 2.4 replaced experimental schedules with experimental automations. */
    suspend fun automations(sessionID: String): List<ScheduleItem> = withContext(Dispatchers.IO) {
        val payload = try {
            getJson("/api/experimental/automation?sessionID=${Uri.encode(sessionID)}")
        } catch (e: ApiException) {
            if (e.status == 403) throw AutomationsDisabledException() else throw e
        }
        parseScheduleItems(payload, "trigger")
    }

    suspend fun schedules(sessionID: String): List<ScheduleItem> = withContext(Dispatchers.IO) {
        val payload = try {
            getJson("/api/experimental/schedule?sessionID=${Uri.encode(sessionID)}")
        } catch (e: ApiException) {
            throw scheduleFailure(e)
        }
        parseScheduleItems(payload, "when")
    }

    suspend fun createAutomation(
        sessionID: String,
        prompt: String,
        trigger: JSONObject
    ): Unit = withContext(Dispatchers.IO) {
        val action = JSONObject()
            .put("type", "prompt")
            .put("prompt", prompt)
            .put("to", JSONObject().put("session", sessionID))
        val body = JSONObject()
            .put("sessionID", sessionID)
            .put("name", prompt.replace('\n', ' ').take(80))
            .put("trigger", trigger)
            .put("action", action)
        try {
            postJson("/api/experimental/automation", body.toString())
        } catch (e: ApiException) {
            if (e.status == 403) throw AutomationsDisabledException() else throw e
        }
        Unit
    }

    suspend fun setAutomationPaused(automationID: String, paused: Boolean): Unit =
        withContext(Dispatchers.IO) {
            val action = if (paused) "pause" else "resume"
            postEmpty("/api/experimental/automation/$automationID/$action", "{}")
        }

    suspend fun deleteAutomation(automationID: String): Unit = withContext(Dispatchers.IO) {
        deleteEmpty("/api/experimental/automation/$automationID")
    }

    suspend fun createSchedule(
        sessionID: String,
        prompt: String,
        whenSpec: JSONObject
    ): Unit = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("sessionID", sessionID)
            .put("prompt", prompt)
            .put("when", whenSpec)
        try {
            postJson("/api/experimental/schedule", body.toString())
        } catch (e: ApiException) {
            throw scheduleFailure(e)
        }
        Unit
    }

    suspend fun setSchedulePaused(scheduleID: String, paused: Boolean): Unit =
        withContext(Dispatchers.IO) {
            val action = if (paused) "pause" else "resume"
            postEmpty("/api/experimental/schedule/$scheduleID/$action", "{}")
        }

    suspend fun deleteSchedule(scheduleID: String): Unit = withContext(Dispatchers.IO) {
        deleteEmpty("/api/experimental/schedule/$scheduleID")
    }

    // ---- Workspace surfaces ------------------------------------------------

    suspend fun jobs(sessionID: String): List<JobItem> = withContext(Dispatchers.IO) {
        val data = getJson("/api/experimental/job?sessionID=${Uri.encode(sessionID)}")
            .optJSONArray("data") ?: JSONArray()
        (0 until data.length()).mapNotNull { i ->
            val o = data.optJSONObject(i) ?: return@mapNotNull null
            JobItem(
                id = o.optString("id"),
                kind = o.optString("kind"),
                title = o.optString("title").ifBlank { o.optString("command") },
                status = o.optString("status"),
                sessionID = o.optString("sessionID"),
                startedAt = o.optJSONObject("time")?.optLong("started") ?: 0L
            )
        }
    }

    suspend fun cancelJob(jobID: String): Unit = withContext(Dispatchers.IO) {
        postEmpty("/api/experimental/job/$jobID/cancel", "{}")
    }

    suspend fun contextSummary(sessionID: String): ContextSummary =
        withContext(Dispatchers.IO) {
            val data = getJson("/api/session/$sessionID/context").optJSONArray("data")
                ?: JSONArray()
            val counts = mutableMapOf<String, Int>()
            var input = 0L
            var output = 0L
            var cost = 0.0
            var compactionSummary = ""
            var compactionRecent = ""
            val entries = mutableListOf<ContextEntry>()
            for (i in 0 until data.length()) {
                val o = data.optJSONObject(i) ?: continue
                val type = o.optString("type")
                counts[type] = (counts[type] ?: 0) + 1
                val tokens = o.optJSONObject("tokens")
                input += tokens?.optLong("input") ?: 0L
                output += tokens?.optLong("output") ?: 0L
                cost += o.optDouble("cost", 0.0)
                if (type == "compaction") {
                    compactionSummary = o.optString("summary")
                    compactionRecent = o.optJSONObject("metadata")?.optString("recent").orEmpty()
                }
                val text = when (type) {
                    "user" -> o.optString("text")
                    "assistant" -> assistantContextText(o)
                    "synthetic" -> o.optString("text").ifBlank { o.optString("summary") }
                    "compaction" -> o.optString("summary")
                    else -> ""
                }
                if (text.isNotBlank()) {
                    entries.add(
                        ContextEntry(
                            type = type,
                            text = text.replace(Regex("\\s+"), " ").trim(),
                            created = o.optJSONObject("time")?.optLong("created") ?: 0L
                        )
                    )
                }
            }
            ContextSummary(
                total = data.length(),
                counts = counts,
                inputTokens = input,
                outputTokens = output,
                cost = cost,
                compactionSummary = compactionSummary,
                compactionRecent = compactionRecent,
                recent = entries.takeLast(24)
            )
        }

    suspend fun savedPermissions(): List<SavedPermission> = withContext(Dispatchers.IO) {
        val data = getJson("/api/permission/saved").optJSONArray("data") ?: JSONArray()
        (0 until data.length()).mapNotNull { i ->
            val o = data.optJSONObject(i) ?: return@mapNotNull null
            SavedPermission(
                id = o.optString("id"),
                action = o.optString("action"),
                resource = o.optString("resource"),
                effect = o.optString("effect")
            )
        }
    }

    suspend fun deleteSavedPermission(id: String): Unit = withContext(Dispatchers.IO) {
        deleteEmpty("/api/permission/saved/$id")
    }

    suspend fun instructions(sessionID: String): List<InstructionEntry> =
        withContext(Dispatchers.IO) {
            val data = getJson("/api/experimental/session/$sessionID/instructions/entries")
                .optJSONArray("data") ?: JSONArray()
            (0 until data.length()).mapNotNull { i ->
                val o = data.optJSONObject(i) ?: return@mapNotNull null
                InstructionEntry(
                    key = o.optString("key"),
                    value = o.optString("value")
                )
            }
        }

    suspend fun putInstruction(sessionID: String, key: String, value: String): Unit =
        withContext(Dispatchers.IO) {
            putEmpty(
                "/api/experimental/session/$sessionID/instructions/entries/${Uri.encode(key)}",
                JSONObject().put("value", value).toString()
            )
        }

    suspend fun deleteInstruction(sessionID: String, key: String): Unit =
        withContext(Dispatchers.IO) {
            deleteEmpty(
                "/api/experimental/session/$sessionID/instructions/entries/${Uri.encode(key)}"
            )
        }

    suspend fun exportSession(sessionID: String): JSONObject = withContext(Dispatchers.IO) {
        getJson("/api/experimental/session/$sessionID/export").optJSONObject("data")
            ?: throw ApiException(500, "empty session export")
    }

    suspend fun importSession(bundle: JSONObject): String = withContext(Dispatchers.IO) {
        val info = bundle.optJSONObject("info") ?: JSONObject()
        val body = JSONObject()
            .put("info", info)
            .put("messages", bundle.optJSONArray("messages") ?: JSONArray())
        val location = bundle.optJSONObject("location")
            ?: info.optJSONObject("location")
        if (location != null) body.put("location", location)
        postJson("/api/experimental/session/import", body.toString())
            .optJSONObject("data")
            ?.optString("id")
            .orEmpty()
    }

    suspend fun vcsStatus(directory: String): List<VcsChange> = withContext(Dispatchers.IO) {
        val data = getJson("/api/vcs/status${locationQuery(directory)}")
            .optJSONArray("data") ?: JSONArray()
        (0 until data.length()).mapNotNull { i ->
            val o = data.optJSONObject(i) ?: return@mapNotNull null
            val rawStatus = o.opt("status")
            VcsChange(
                path = o.optString("path").ifBlank { o.optString("file") },
                status = when (rawStatus) {
                    is String -> rawStatus
                    is JSONObject -> rawStatus.optString("type")
                        .ifBlank { rawStatus.optString("status") }
                    else -> ""
                },
                additions = o.optInt("additions"),
                deletions = o.optInt("deletions")
            )
        }
    }

    suspend fun vcsBranches(directory: String): List<String> = withContext(Dispatchers.IO) {
        val data = getJson("/api/vcs/branch${locationQuery(directory)}")
            .optJSONArray("data") ?: JSONArray()
        (0 until data.length()).mapNotNull { i ->
            when (val value = data.opt(i)) {
                is String -> value.takeIf { it.isNotBlank() }
                is JSONObject -> value.optString("name").takeIf { it.isNotBlank() }
                else -> null
            }
        }
    }

    suspend fun worktrees(projectID: String): List<WorktreeEntry> =
        withContext(Dispatchers.IO) {
            val payload = getJson("/api/worktree?projectID=${Uri.encode(projectID)}")
            val out = mutableListOf<WorktreeEntry>()
            when (val data = payload.opt("data")) {
                is JSONArray -> for (i in 0 until data.length()) {
                    val o = data.optJSONObject(i)
                    if (o != null) {
                        out.add(WorktreeEntry(o.optString("directory"), o.optString("strategy")))
                    } else {
                        data.optJSONArray(i)?.let { pair ->
                            out.add(WorktreeEntry(pair.optString(0), pair.optString(1)))
                        }
                    }
                }
                is JSONObject -> out.add(
                    WorktreeEntry(data.optString("directory"), data.optString("strategy"))
                )
            }
            out.filter { it.directory.isNotBlank() }
        }

    suspend fun createWorktree(projectID: String, name: String): Unit =
        withContext(Dispatchers.IO) {
            postJson(
                "/api/worktree",
                JSONObject().put("projectID", projectID).put("name", name).toString()
            )
            Unit
        }

    suspend fun removeWorktree(projectID: String, directory: String, force: Boolean): Unit =
        withContext(Dispatchers.IO) {
            deleteWithBody(
                "/api/worktree",
                JSONObject()
                    .put("projectID", projectID)
                    .put("directory", directory)
                    .put("force", force)
                    .toString()
            )
        }

    suspend fun usageLimits(directory: String): String? = withContext(Dispatchers.IO) {
        val data = getJson("/api/experimental/usage/limits${locationQuery(directory)}").opt("data")
        when {
            data == null || data == JSONObject.NULL -> null
            data is JSONObject -> data.toString(2)
            data is JSONArray -> data.toString(2)
            else -> data.toString()
        }
    }

    suspend fun mcpServers(): List<McpServer> = withContext(Dispatchers.IO) {
        val data = getJson("/api/mcp").optJSONArray("data") ?: JSONArray()
        (0 until data.length()).map { i ->
            val o = data.getJSONObject(i)
            McpServer(
                name = o.optString("name"),
                status = o.optJSONObject("status")?.optString("status").orEmpty()
            )
        }
    }

    suspend fun mcpConnect(name: String): Unit = withContext(Dispatchers.IO) {
        postEmpty("/api/experimental/mcp/$name/connect", "{}")
    }

    suspend fun mcpDisconnect(name: String): Unit = withContext(Dispatchers.IO) {
        postEmpty("/api/experimental/mcp/$name/disconnect", "{}")
    }

    suspend fun mcpResources(): List<McpResource> = withContext(Dispatchers.IO) {
        val data = getJson("/api/mcp/resource").optJSONObject("data") ?: return@withContext emptyList()
        val out = mutableListOf<McpResource>()
        val resources = data.optJSONArray("resources") ?: JSONArray()
        for (i in 0 until resources.length()) {
            val o = resources.optJSONObject(i) ?: continue
            out += McpResource(
                server = o.optString("server"),
                name = o.optString("name"),
                uri = o.optString("uri"),
                description = o.optString("description")
            )
        }
        val templates = data.optJSONArray("templates") ?: JSONArray()
        for (i in 0 until templates.length()) {
            val o = templates.optJSONObject(i) ?: continue
            out += McpResource(
                server = o.optString("server"),
                name = o.optString("name") + " (template)",
                uri = o.optString("uriTemplate"),
                description = o.optString("description")
            )
        }
        out
    }

    suspend fun integrations(): List<IntegrationItem> = withContext(Dispatchers.IO) {
        val data = getJson("/api/integration").optJSONArray("data") ?: JSONArray()
        (0 until data.length()).map { i ->
            val o = data.getJSONObject(i)
            val methodsArr = o.optJSONArray("methods") ?: JSONArray()
            val methods = (0 until methodsArr.length()).mapNotNull { m ->
                methodsArr.optJSONObject(m)?.optString("type")
            }
            val envArr = (0 until methodsArr.length())
                .mapNotNull { methodsArr.optJSONObject(it) }
                .firstOrNull { it.optString("type") == "env" }
                ?.optJSONArray("names")
            val envNames = (0 until (envArr?.length() ?: 0)).map { envArr!!.optString(it) }
            IntegrationItem(
                id = o.optString("id"),
                name = o.optString("name").ifBlank { o.optString("id") },
                connected = (o.optJSONArray("connections")?.length() ?: 0) > 0,
                methods = methods,
                envNames = envNames
            )
        }
    }

    suspend fun connectIntegrationKey(integrationID: String, key: String): Unit =
        withContext(Dispatchers.IO) {
            postEmpty(
                "/api/integration/$integrationID/connect/key",
                JSONObject().put("key", key).toString()
            )
        }

    /** Shell commands are location-scoped in V2; the session id rides in metadata. */
    private fun locationQuery(directory: String): String =
        if (directory.isBlank()) "" else "?location[directory]=" + Uri.encode(directory)

    suspend fun runningShells(directory: String): List<RunningShell> =
        withContext(Dispatchers.IO) {
            val data = getJson("/api/shell${locationQuery(directory)}").optJSONArray("data")
                ?: JSONArray()
            (0 until data.length()).mapNotNull { i ->
                val o = data.optJSONObject(i) ?: return@mapNotNull null
                val id = o.optString("id")
                if (id.isBlank()) return@mapNotNull null
                RunningShell(
                    id = id,
                    command = o.optString("command"),
                    status = o.optString("status"),
                    startedAt = o.optJSONObject("time")?.optLong("started") ?: 0L,
                    sessionID = o.optJSONObject("metadata")?.optString("sessionID").orEmpty()
                )
            }
        }

    suspend fun runShell(sessionID: String, directory: String, command: String): ShellRun =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("command", command)
            if (directory.isNotBlank()) body.put("cwd", directory)
            body.put("metadata", JSONObject().put("sessionID", sessionID))
            val data = postJson("/api/shell${locationQuery(directory)}", body.toString())
                .getJSONObject("data")
            ShellRun(data.optString("id"), data.optString("status"))
        }

    suspend fun shellOutput(shellID: String, directory: String): String =
        withContext(Dispatchers.IO) {
            getJson("/api/shell/$shellID/output${locationQuery(directory)}")
                .optJSONObject("data")
                ?.optString("output")
                .orEmpty()
        }

    suspend fun stopShell(shellID: String, directory: String): Unit =
        withContext(Dispatchers.IO) {
            deleteEmpty("/api/shell/$shellID${locationQuery(directory)}")
        }

    suspend fun vcsInfo(directory: String): VcsInfo? = withContext(Dispatchers.IO) {
        val query = if (directory.isBlank()) "" else "?location[directory]=" + Uri.encode(directory)
        val data = getJson("/api/vcs$query").optJSONObject("data") ?: return@withContext null
        val branch = data.optJSONObject("branch")
        VcsInfo(
            provider = data.optString("provider"),
            branch = branch?.optString("current").orEmpty(),
            defaultBranch = branch?.optString("default").orEmpty()
        )
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
