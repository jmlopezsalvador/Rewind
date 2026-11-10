package com.rewind.ui

import com.rewind.time.Direction
import com.rewind.time.TimeFormat

interface KeyValueStore {
    fun getString(key: String): String?
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getLong(key: String, default: Long): Long
    fun putString(key: String, value: String)
    fun putBoolean(key: String, value: Boolean)
    fun putLong(key: String, value: Long)
}

/** Ajustes de la actuación (SPEC 5). Se irán ampliando. */
data class AppSettings(
    val direction: Direction = Direction.BACKWARDS,
    val extraMinutes: Int = 3,
    val timeFormat: TimeFormat = TimeFormat.H24_TWO_DIGITS,
)

class SettingsStore(private val prefs: KeyValueStore) {
    fun load(): AppSettings = AppSettings(
        direction = prefs.getString("direction")?.let { runCatching { Direction.valueOf(it) }.getOrNull() }
            ?: Direction.BACKWARDS,
        extraMinutes = prefs.getLong("extraMinutes", 3).toInt().coerceIn(1, 999),
        timeFormat = prefs.getString("timeFormat")?.let { runCatching { TimeFormat.valueOf(it) }.getOrNull() }
            ?: TimeFormat.H24_TWO_DIGITS,
    )

    fun save(s: AppSettings) {
        prefs.putString("direction", s.direction.name)
        prefs.putLong("extraMinutes", s.extraMinutes.toLong())
        prefs.putString("timeFormat", s.timeFormat.name)
    }
}
