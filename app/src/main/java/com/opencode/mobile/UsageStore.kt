package com.opencode.mobile

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Daily spend history, accumulated from the cumulative per-session cost the
 * server reports. Only positive deltas are added, so a session's running total
 * is never double-counted.
 */
object UsageStore {

    private const val FILE = "opencode_usage"
    private const val KEY_LAST = "last_cost"
    private const val KEY_DAYS = "days"
    private const val KEEP_DAYS = 180

    data class Day(val label: String, val cost: Double)

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun dayKey(time: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(time))

    @Synchronized
    fun record(context: Context, sessions: List<SessionItem>) {
        if (sessions.isEmpty()) return
        val p = prefs(context)
        val last = runCatching {
            JSONObject(p.getString(KEY_LAST, "{}") ?: "{}")
        }.getOrDefault(JSONObject())
        val days = runCatching {
            JSONObject(p.getString(KEY_DAYS, "{}") ?: "{}")
        }.getOrDefault(JSONObject())

        val today = dayKey()
        val todayObj = days.optJSONObject(today) ?: JSONObject().also { days.put(today, it) }
        var total = todayObj.optDouble("total", 0.0)
        var changed = false

        sessions.forEach { session ->
            val previous = last.optDouble(session.id, -1.0)
            last.put(session.id, session.cost)
            if (previous < 0.0) return@forEach // first sighting: no history to charge
            val delta = session.cost - previous
            if (delta > 0.0) {
                total += delta
                changed = true
            }
        }

        if (changed) todayObj.put("total", total)

        // Drop days that fell out of the window.
        val cutoff = System.currentTimeMillis() - KEEP_DAYS * 24L * 60L * 60_000L
        val stale = mutableListOf<String>()
        val keys = days.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val parsed = runCatching {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(key)?.time ?: 0L
            }.getOrDefault(0L)
            if (parsed in 1 until cutoff) stale += key
        }
        stale.forEach { days.remove(it) }

        p.edit()
            .putString(KEY_LAST, last.toString())
            .putString(KEY_DAYS, days.toString())
            .apply()
    }

    fun today(context: Context): Double =
        prefs(context).getString(KEY_DAYS, null)
            ?.let { runCatching { JSONObject(it) }.getOrNull() }
            ?.optJSONObject(dayKey())
            ?.optDouble("total", 0.0)
            ?: 0.0

    /** Sum of this calendar month's recorded days. */
    fun monthToDate(context: Context): Double {
        val days = prefs(context).getString(KEY_DAYS, null)
            ?.let { runCatching { JSONObject(it) }.getOrNull() }
            ?: return 0.0
        val prefix = dayKey().take(7) // yyyy-MM
        var total = 0.0
        val keys = days.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key.startsWith(prefix)) {
                total += days.optJSONObject(key)?.optDouble("total", 0.0) ?: 0.0
            }
        }
        return total
    }

    /** Month-to-date plus the recent daily average for the rest of the month. */
    fun projectedMonth(context: Context): Double {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = now
        val remaining = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH) -
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val average = recent(context, 7).sumOf { it.cost } / 7.0
        return monthToDate(context) + average * remaining
    }

    /** CSV of every stored day, oldest first. */
    fun csv(context: Context): String {
        val days = prefs(context).getString(KEY_DAYS, null)
            ?.let { runCatching { JSONObject(it) }.getOrNull() }
            ?: return "date,cost\n"
        val keys = days.keys().asSequence().sorted()
        return buildString {
            appendLine("date,cost")
            keys.forEach { key ->
                append(key).append(',').append(days.optJSONObject(key)?.optDouble("total", 0.0) ?: 0.0)
                appendLine()
            }
        }
    }

    /** Last [count] days, oldest first, including days with no spend. */
    fun recent(context: Context, count: Int = 7): List<Day> {
        val days = prefs(context).getString(KEY_DAYS, null)
            ?.let { runCatching { JSONObject(it) }.getOrNull() }
            ?: JSONObject()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val label = SimpleDateFormat("EEE", Locale.getDefault())
        val now = System.currentTimeMillis()
        return (count - 1 downTo 0).map { offset ->
            val time = now - offset * 24L * 60L * 60_000L
            val key = format.format(Date(time))
            Day(
                label = label.format(Date(time)),
                cost = days.optJSONObject(key)?.optDouble("total", 0.0) ?: 0.0
            )
        }
    }
}
