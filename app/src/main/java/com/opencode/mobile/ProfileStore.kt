package com.opencode.mobile

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class ServerProfile(
    val id: String,
    val name: String,
    val server: String,
    val password: String,
    val fallback: String,
    val themeMode: String = "",
    val accent: String = "",
    val persistentWatch: Boolean = false,
    val watchdogMinutes: Int = 0
)

/**
 * Named server configurations. The active profile is mirrored into the legacy
 * `server`/`password`/`fallback_server` keys so the rest of the app keeps one
 * source of truth.
 */
object ProfileStore {

    private const val KEY = "server_profiles"
    private const val ACTIVE = "active_profile"

    /** Bumped whenever the active profile changes, so the UI can reload. */
    val revision = MutableStateFlow(0)

    fun all(context: Context): List<ServerProfile> {
        val raw = SecurePrefs.get(context).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                ServerProfile(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    server = o.optString("server"),
                    password = o.optString("password"),
                    fallback = o.optString("fallback"),
                    themeMode = o.optString("themeMode"),
                    accent = o.optString("accent"),
                    persistentWatch = o.optBoolean("persistentWatch", false),
                    watchdogMinutes = o.optInt("watchdogMinutes", 0)
                )
            }
        }.getOrDefault(emptyList())
    }

    fun activeID(context: Context): String? =
        SecurePrefs.get(context).getString(ACTIVE, null)

    fun active(context: Context): ServerProfile? =
        all(context).firstOrNull { it.id == activeID(context) }

    /** Creates a first profile from the current saved connection, if needed. */
    @Synchronized
    fun ensure(context: Context): ServerProfile? {
        val prefs = SecurePrefs.get(context)
        val existing = all(context)
        if (existing.isNotEmpty()) {
            return existing.firstOrNull { it.id == activeID(context) } ?: existing.first()
        }
        val server = prefs.getString("server", "").orEmpty()
        if (server.isBlank()) return null
        val profile = ServerProfile(
            id = newId(),
            name = "Default",
            server = server,
            password = prefs.getString("password", "").orEmpty(),
            fallback = prefs.getString("fallback_server", "").orEmpty(),
            themeMode = UiPrefs.mode.value,
            accent = UiPrefs.accent.value,
            persistentWatch = prefs.getBoolean("watch_persistent", false),
            watchdogMinutes = prefs.getInt("watchdog_minutes", 0)
        )
        write(context, listOf(profile))
        prefs.edit().putString(ACTIVE, profile.id).apply()
        return profile
    }

    @Synchronized
    fun save(context: Context, profile: ServerProfile) {
        val next = all(context).filterNot { it.id == profile.id } + profile
        write(context, next)
    }

    /** Stores the current connection under [name] and makes it active. */
    @Synchronized
    fun addFromCurrent(context: Context, name: String): ServerProfile {
        val prefs = SecurePrefs.get(context)
        val profile = ServerProfile(
            id = newId(),
            name = name.ifBlank { "Server" },
            server = prefs.getString("server", "").orEmpty(),
            password = prefs.getString("password", "").orEmpty(),
            fallback = prefs.getString("fallback_server", "").orEmpty(),
            themeMode = UiPrefs.mode.value,
            accent = UiPrefs.accent.value,
            persistentWatch = prefs.getBoolean("watch_persistent", false),
            watchdogMinutes = prefs.getInt("watchdog_minutes", 0)
        )
        write(context, all(context) + profile)
        prefs.edit().putString(ACTIVE, profile.id).apply()
        revision.value += 1
        return profile
    }

    @Synchronized
    fun switchTo(context: Context, id: String) {
        val profile = all(context).firstOrNull { it.id == id } ?: return
        SecurePrefs.get(context).edit()
            .putString("server", profile.server)
            .putString("password", profile.password)
            .putString("fallback_server", profile.fallback)
            .putString(ACTIVE, profile.id)
            .putBoolean("watch_persistent", profile.persistentWatch)
            .putInt("watchdog_minutes", profile.watchdogMinutes)
            .apply()
        if (profile.themeMode.isNotBlank()) UiPrefs.setMode(context, profile.themeMode)
        if (profile.accent.isNotBlank()) UiPrefs.setAccent(context, profile.accent)
        revision.value += 1
    }

    @Synchronized
    fun delete(context: Context, id: String) {
        val remaining = all(context).filterNot { it.id == id }
        write(context, remaining)
        if (activeID(context) == id && remaining.isNotEmpty()) {
            switchTo(context, remaining.first().id)
        }
    }

    /** Copies edited connection fields into the active profile. */
    @Synchronized
    fun updateActiveFromPrefs(context: Context) {
        val prefs = SecurePrefs.get(context)
        val id = activeID(context) ?: return
        val profile = all(context).firstOrNull { it.id == id } ?: return
        save(
            context,
            profile.copy(
                server = prefs.getString("server", "").orEmpty(),
                password = prefs.getString("password", "").orEmpty(),
                fallback = prefs.getString("fallback_server", "").orEmpty(),
                themeMode = UiPrefs.mode.value,
                accent = UiPrefs.accent.value,
                persistentWatch = prefs.getBoolean("watch_persistent", false),
                watchdogMinutes = prefs.getInt("watchdog_minutes", 0)
            )
        )
    }

    private fun write(context: Context, profiles: List<ServerProfile>) {
        val arr = JSONArray()
        profiles.forEach { p ->
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("server", p.server)
                    .put("password", p.password)
                    .put("fallback", p.fallback)
                    .put("themeMode", p.themeMode)
                    .put("accent", p.accent)
                    .put("persistentWatch", p.persistentWatch)
                    .put("watchdogMinutes", p.watchdogMinutes)
            )
        }
        SecurePrefs.get(context).edit().putString(KEY, arr.toString()).apply()
    }

    private fun newId(): String = "p${System.currentTimeMillis()}"
}
