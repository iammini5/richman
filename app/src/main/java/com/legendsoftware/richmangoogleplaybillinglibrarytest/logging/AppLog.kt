package com.legendsoftware.richmangoogleplaybillinglibrarytest.logging

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

/**
 * Device-local diagnostics log. Keep messages sanitized: do not log purchase
 * tokens, receipts, email addresses, or other sensitive data.
 */
object AppLog {
    private const val TAG = "RichmanLog"
    private const val PREFS = "richman_app_log"
    private const val ENTRIES = "entries"
    private const val MAX_ENTRIES = 200

    private lateinit var appContext: Context

    data class Entry(
        val timestamp: String,
        val level: String,
        val category: String,
        val message: String,
    )

    fun initialize(context: Context) {
        appContext = context.applicationContext
        info("app", "Application initialized")
    }

    fun info(category: String, message: String) = write("INFO", category, message)
    fun warn(category: String, message: String) = write("WARN", category, message)
    fun error(category: String, message: String) = write("ERROR", category, message)

    fun entries(): List<Entry> {
        if (!::appContext.isInitialized) return emptyList()
        val raw = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(ENTRIES, "[]")
            .orEmpty()

        return runCatching {
            val values = JSONArray(raw)
            buildList {
                for (index in values.length() - 1 downTo 0) {
                    val value = values.getJSONObject(index)
                    add(
                        Entry(
                            timestamp = value.optString("timestamp"),
                            level = value.optString("level"),
                            category = value.optString("category"),
                            message = value.optString("message"),
                        ),
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    fun clear() {
        if (!::appContext.isInitialized) return
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(ENTRIES)
            .apply()
        Log.i(TAG, "In-app log cleared")
    }

    private fun write(level: String, category: String, message: String) {
        when (level) {
            "WARN" -> Log.w(TAG, "[$category] $message")
            "ERROR" -> Log.e(TAG, "[$category] $message")
            else -> Log.i(TAG, "[$category] $message")
        }

        if (!::appContext.isInitialized) return

        runCatching {
            val preferences = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val entries = JSONArray(preferences.getString(ENTRIES, "[]"))
            while (entries.length() >= MAX_ENTRIES) entries.remove(0)
            entries.put(
                JSONObject()
                    .put("timestamp", DateFormat.getDateTimeInstance().format(Date()))
                    .put("level", level)
                    .put("category", category)
                    .put("message", message),
            )
            preferences.edit().putString(ENTRIES, entries.toString()).apply()
        }.onFailure {
            Log.e(TAG, "Unable to persist in-app log entry", it)
        }
    }
}
