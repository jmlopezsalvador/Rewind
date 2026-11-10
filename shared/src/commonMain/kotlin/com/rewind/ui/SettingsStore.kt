package com.rewind.ui

import com.rewind.time.Direction
import com.rewind.display.DisplayConfig
import com.rewind.time.AnimationSpeed
import com.rewind.time.AodStyle
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
    /** Permite entrar en el modo de registro de número con una pulsación larga en la casilla inferior izquierda. */
    val numberEntry: Boolean = false,
    /** Muestra el teclado en pantalla durante el registro (invisible: la cuadrícula existe pero no se ve). */
    val keypadVisible: Boolean = false,
    /** Opacidad (0.1..0.9) del teclado cuando es visible. */
    val keypadOpacity: Float = 0.35f,
    /** Dibuja encima de la pantalla principal la cuadrícula 3×3 con la función de cada casilla (ayuda). */
    val showControls: Boolean = false,
    /** Aspecto de la pantalla "apagada" (Always On). */
    val aodStyle: AodStyle = AodStyle.BLACK,
    /** Brillo (0.1..0.9) de la pantalla atenuada respecto a la normal. */
    val dimLevel: Float = 0.35f,
    /** Desenfoque (0..30 dp) del fondo cuando está atenuada. En Android anterior a 12 no se aplica. */
    val dimBlur: Int = 12,
    /** Segundos que la pantalla atenuada se queda despierta tras tocarla. */
    val awakeSeconds: Int = 5,
) {
    fun toConfig() = PerformanceConfig(
        direction, extraMinutes, animate, delaySeconds * 1000L, speed, alwaysOn, alwaysOn && autoWake, aodStyle, awakeSeconds,
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
        trigger = prefs.getString("trigger")?.let { runCatching { Trigger.valueOf(it) }.getOrNull() }
            ?.takeUnless { it == Trigger.VOLUME && !platformSupportsVolumeKeys() } ?: Trigger.TAP,
        alwaysOn = prefs.getBoolean("alwaysOn", false),
        autoWake = prefs.getBoolean("autoWake", false),
        minuteAlert = prefs.getBoolean("minuteAlert", false),
        alertSeconds = prefs.getLong("alertSeconds", 10).toInt().coerceIn(3, 30),
        numberEntry = prefs.getBoolean("numberEntry", false),
        keypadVisible = prefs.getBoolean("keypadVisible", false),
        keypadOpacity = (prefs.getLong("keypadOpacityPct", 35).toInt() / 100f).coerceIn(0.1f, 0.9f),
        showControls = prefs.getBoolean("showControls", false),
        aodStyle = prefs.getString("aodStyle")?.let { runCatching { AodStyle.valueOf(it) }.getOrNull() } ?: AodStyle.BLACK,
        dimLevel = (prefs.getLong("dimLevelPct", 35).toInt() / 100f).coerceIn(0.1f, 0.9f),
        dimBlur = prefs.getLong("dimBlur", 12).toInt().coerceIn(0, 30),
        awakeSeconds = prefs.getLong("awakeSeconds", 5).toInt().coerceIn(3, 30),
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
        prefs.putBoolean("numberEntry", s.numberEntry)
        prefs.putBoolean("keypadVisible", s.keypadVisible)
        prefs.putLong("keypadOpacityPct", (s.keypadOpacity * 100).toInt().toLong())
        prefs.putBoolean("showControls", s.showControls)
        prefs.putString("aodStyle", s.aodStyle.name)
        prefs.putLong("dimLevelPct", (s.dimLevel * 100).toInt().toLong())
        prefs.putLong("dimBlur", s.dimBlur.toLong())
        prefs.putLong("awakeSeconds", s.awakeSeconds.toLong())
    }
}
