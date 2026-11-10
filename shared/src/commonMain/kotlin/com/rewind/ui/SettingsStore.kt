package com.rewind.ui

import com.rewind.time.Direction
import com.rewind.display.DisplayConfig
import com.rewind.time.AnimationSpeed
import com.rewind.time.DateFormat
import com.rewind.time.PerformanceConfig
import com.rewind.time.TimeFormat
import com.rewind.time.Trigger

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
    val trigger: Trigger = Trigger.TAP,
    val alwaysOn: Boolean = false,
    val autoWake: Boolean = false,
    val minuteAlert: Boolean = false,
    val alertSeconds: Int = 10,
    /** Permite fijar N en directo pulsando varias veces la casilla izquierda-centro de la pantalla. */
    val gridInput: Boolean = false,
    /** Dibuja encima de la pantalla principal la cuadrícula 3×3 con la función de cada casilla (ayuda). */
    val showControls: Boolean = false,
) {
    fun toConfig() = PerformanceConfig(
        direction, extraMinutes, animate, delaySeconds * 1000L, speed, alwaysOn, alwaysOn && autoWake,
    )
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
        trigger = prefs.getString("trigger")?.let { runCatching { Trigger.valueOf(it) }.getOrNull() } ?: Trigger.TAP,
        alwaysOn = prefs.getBoolean("alwaysOn", false),
        autoWake = prefs.getBoolean("autoWake", false),
        minuteAlert = prefs.getBoolean("minuteAlert", false),
        alertSeconds = prefs.getLong("alertSeconds", 10).toInt().coerceIn(3, 30),
        gridInput = prefs.getBoolean("gridInput", false),
        showControls = prefs.getBoolean("showControls", false),
    )

    fun save(s: AppSettings) {
        prefs.putString("direction", s.direction.name)
        prefs.putLong("extraMinutes", s.extraMinutes.toLong())
        prefs.putString("timeFormat", s.timeFormat.name)
        prefs.putBoolean("animate", s.animate)
        prefs.putLong("delaySeconds", s.delaySeconds.toLong())
        prefs.putString("speed", s.speed.name)
        prefs.putString("display", s.display.encode())
        prefs.putString("trigger", s.trigger.name)
        prefs.putBoolean("alwaysOn", s.alwaysOn)
        prefs.putBoolean("autoWake", s.autoWake)
        prefs.putBoolean("minuteAlert", s.minuteAlert)
        prefs.putLong("alertSeconds", s.alertSeconds.toLong())
        prefs.putBoolean("gridInput", s.gridInput)
        prefs.putBoolean("showControls", s.showControls)
    }
}
