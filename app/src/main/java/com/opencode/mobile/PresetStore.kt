package com.opencode.mobile

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** A saved agent + model + opening prompt, stored locally. */
data class SessionPreset(
    val id: String,
    val name: String,
    val agentID: String,
    val modelID: String,
    val providerID: String,
    val prompt: String
)

object PresetStore {

    private const val KEY = "presets"

    fun all(context: Context): List<SessionPreset> {
        val raw = SecurePrefs.get(context).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                SessionPreset(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    agentID = o.optString("agentID"),
                    modelID = o.optString("modelID"),
                    providerID = o.optString("providerID"),
                    prompt = o.optString("prompt")
                )
            }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, preset: SessionPreset) {
        val next = (all(context).filterNot { it.id == preset.id } + preset).takeLast(20)
        write(context, next)
    }

    fun delete(context: Context, id: String) {
        write(context, all(context).filterNot { it.id == id })
    }

    private fun write(context: Context, presets: List<SessionPreset>) {
        val arr = JSONArray()
        presets.forEach { p ->
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("agentID", p.agentID)
                    .put("modelID", p.modelID)
                    .put("providerID", p.providerID)
                    .put("prompt", p.prompt)
            )
        }
        SecurePrefs.get(context).edit().putString(KEY, arr.toString()).apply()
    }
}
