package com.opencode.mobile

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Last known model catalog. The context meter needs a model's window size, and a
 * single failed catalog fetch used to leave it blank for the rest of the session.
 * Limits are per model, so one cached catalog is enough to restore the meter.
 */
object ModelCache {

    private const val PREFS = "model_cache"
    private const val KEY = "models"

    fun save(context: Context, models: List<ModelOption>) {
        if (models.isEmpty()) return
        runCatching {
            val array = JSONArray()
            models.forEach { model ->
                array.put(
                    JSONObject()
                        .put("id", model.id)
                        .put("providerID", model.providerID)
                        .put("name", model.name)
                        .put("family", model.family)
                        .put("contextLimit", model.contextLimit)
                        .put("providerName", model.providerName)
                )
            }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY, array.toString())
                .apply()
        }
    }

    fun load(context: Context): List<ModelOption> = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null)
            ?: return emptyList()
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            array.optJSONObject(index)?.let { o ->
                ModelOption(
                    id = o.optString("id"),
                    providerID = o.optString("providerID"),
                    name = o.optString("name"),
                    family = o.optString("family"),
                    contextLimit = o.optLong("contextLimit"),
                    providerName = o.optString("providerName")
                )
            }
        }
    }.getOrDefault(emptyList())
}
