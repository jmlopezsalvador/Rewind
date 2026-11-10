package com.rewind.ui

import com.rewind.time.Direction
import com.rewind.display.DisplayConfig
import com.rewind.time.AnimationSpeed
import com.rewind.time.DateFormat
import com.rewind.time.PerformanceConfig
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
    val animate: Boolean = true,
    val delaySeconds: Int = 4,
    val speed: AnimationSpeed = AnimationSpeed.REGULAR,
    val display: DisplayConfig = DisplayConfig(),
) {
    fun toConfig() = PerformanceConfig(direction, extraMinutes, animate, delaySeconds * 1000L, speed)
}

class SettingsStore(private val prefs: KeyValueStore) {
    fun load(): AppSettings = AppSettings(
        direction = prefs.getString("direction")?.let { runCatching { Direction.valueOf(it) }.getOrNull() }
            ?: Direction.BACKWARDS,
        extraMinutes = prefs.getLong("extraMinutes", 3).toInt().coerceIn(1, 999),
        timeFormat = prefs.getString("timeFormat")?.let { runCatching { TimeFormat.valueOf(it) }.getOrNull() }
            ?: TimeFormat.H24_TWO_DIGITS,
        animate = prefs.getBoolean("animate", true),
        delaySeconds = prefs.getLong("delaySeconds", 4).toInt().coerceIn(1, 10),
        speed = prefs.getString("speed")?.let { runCatching { AnimationSpeed.valueOf(it) }.getOrNull() }
            ?: AnimationSpeed.REGULAR,
        display = DisplayConfig.decode(prefs.getString("display"))
            ?: DisplayConfig(dateFormat = if (platformLanguage() == "es") DateFormat.ES_LONG else DateFormat.EN_SHORT),
    )

    fun save(s: AppSettings) {
        prefs.putString("direction", s.direction.name)
        prefs.putLong("extraMinutes", s.extraMinutes.toLong())
        prefs.putString("timeFormat", s.timeFormat.name)
        prefs.putBoolean("animate", s.animate)
        prefs.putLong("delaySeconds", s.delaySeconds.toLong())
        prefs.putString("speed", s.speed.name)
        prefs.putString("display", s.display.encode())
    }
}
