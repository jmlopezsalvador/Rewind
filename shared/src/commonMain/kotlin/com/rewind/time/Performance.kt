package com.rewind.time

import kotlinx.datetime.Instant
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

/** Tiempo por paso de minuto (SPEC 5; valores propuestos, ver Q4). */
enum class AnimationSpeed(val stepMs: Long) { SLOW(1000), REGULAR(500), FAST(200) }

/** Aspecto de la pantalla "apagada" (Always On): negra del todo, o atenuada y desenfocada como un móvil bloqueado. */
enum class AodStyle { BLACK, DIMMED }

/** Cómo se dispara el efecto (SPEC 7). VOLUME solo existe en Android (ver `platformSupportsVolumeKeys`). */
enum class Trigger { TAP, TAP_VIBRATE, PROXIMITY, VOLUME }

data class PerformanceConfig(
    val direction: Direction = Direction.BACKWARDS,
    val extraMinutes: Int = 3,
    val animate: Boolean = true,
    val delayMs: Long = 4_000,
    val speed: AnimationSpeed = AnimationSpeed.REGULAR,
    /** Simula el móvil bloqueado y con la pantalla apagada hasta que se activa el efecto. */
    val alwaysOn: Boolean = false,
    /** Con [alwaysOn]: la pantalla se "enciende" al empezar el retardo. Sin él, se enciende al empezar la animación. */
    val autoWake: Boolean = false,
    /** Aspecto de la pantalla mientras está "apagada". */
    val aodStyle: AodStyle = AodStyle.BLACK,
    /** Con [AodStyle.DIMMED]: segundos que la pantalla se queda despierta tras tocarla antes de volver a atenuarse. */
    val awakeSeconds: Int = 5,
)

/**
 * Fases de la actuación (SPEC 6). IDLE/GRID_INPUT llegarán con la cuadrícula (fase 2):
 * con la cuadrícula desactivada se pasa directamente a ARMED.
 */
enum class Phase { ARMED, DELAY, ANIMATING, SYNCED_HOLD }

/**
 * Estado inmutable de la actuación. Todo es función pura de (estado, ahora): sin temporizadores acumulativos,
 * así el reloj falso no deriva y se puede testear con un reloj inyectado.
 */
data class Performance(
    val config: PerformanceConfig,
    val triggeredAt: Instant? = null,
    /** Último toque que despertó la pantalla atenuada (solo con [AodStyle.DIMMED]). */
    val touchedAt: Instant? = null,
) {
    /** Desplazamiento inicial en minutos con signo (Atrás = +N). */
    val startOffset: Int get() = offsetMinutes(config.direction, config.extraMinutes)

    /** Duración de cada paso; con N grande se acelera para que la animación no pase de [MAX_ANIMATION_MS]. */
    val stepMs: Long
        get() {
            val n = abs(startOffset).coerceAtLeast(1)
            return min(config.speed.stepMs, (MAX_ANIMATION_MS / n).coerceAtLeast(1))
        }

    private val animationMs: Long get() = if (config.animate) abs(startOffset) * stepMs else 0L

    /** Dispara el efecto; solo cuenta en ARMED (un segundo disparo no hace nada). */
    fun trigger(now: Instant): Performance = if (triggeredAt == null) copy(triggeredAt = now) else this

    /** Cambia N en directo (cuadrícula). Solo vale mientras se espera el disparo (ARMED). */
    fun withMinutes(n: Int): Performance =
        if (triggeredAt != null) this else copy(config = config.copy(extraMinutes = n.coerceIn(1, 999)))

    /**
     * Vuelve a la espera (reiniciar): se cancela la animación y se recalcula el offset con [newConfig] (por defecto la
     * actual; la interfaz pasa la de Ajustes para descartar el N registrado en directo). Con la pantalla atenuada,
     * reiniciar cuenta como actividad: la pantalla no se atenúa de golpe, solo tras [PerformanceConfig.awakeSeconds]
     * sin tocar (y mantiene el nivel de enfoque que tuviera, para no dar saltos).
     */
    fun reset(now: Instant, newConfig: PerformanceConfig = config): Performance {
        val fresh = Performance(newConfig)
        if (!dimmed || !fresh.dimmed) return fresh
        return fresh.copy(touchedAt = now - (focusAt(now) * WAKE_MS).toLong().milliseconds)
    }

    private val dimmed: Boolean get() = config.alwaysOn && config.aodStyle == AodStyle.DIMMED

    /** Instante en que terminan retardo y animación (empieza SYNCED_HOLD); null si aún no se ha disparado. */
    private fun effectEnd(): Instant? = triggeredAt?.plus((config.delayMs + animationMs).milliseconds)

    /**
     * Enfoque en función del tiempo transcurrido desde el inicio de un ciclo "despierta": sube al empezar
     * ([WAKE_MS]), se mantiene [PerformanceConfig.awakeSeconds] y vuelve a bajar ([DIM_MS]).
     */
    private fun cycleFocus(elapsedMs: Long): Float {
        val hold = config.awakeSeconds * 1000L
        return when {
            elapsedMs < 0 -> 0f
            elapsedMs < WAKE_MS -> elapsedMs.toFloat() / WAKE_MS
            elapsedMs < WAKE_MS + hold -> 1f
            else -> (1f - (elapsedMs - WAKE_MS - hold).toFloat() / DIM_MS).coerceIn(0f, 1f)
        }
    }

    /** Enfoque debido solo al último toque. */
    private fun touchFocusAt(now: Instant): Float = touchedAt?.let { cycleFocus((now - it).inWholeMilliseconds) } ?: 0f

    /**
     * Enfoque mientras corre el efecto (retardo y animación): si la pantalla ya estaba despierta al disparar, se queda
     * despierta (no se atenúa mientras el reloj viaja); si no, se enciende con el disparo o al empezar la animación.
     */
    private fun effectFocusAt(now: Instant, t0: Instant): Float {
        val awakeAtTrigger = touchedAt?.let { cycleFocus((t0 - it).inWholeMilliseconds) } ?: 0f
        return if (awakeAtTrigger >= 0.5f) 1f else max(wakeFractionAt(now), touchFocusAt(now))
    }

    /**
     * Inicio del ciclo "despierta" que rige una vez terminado el efecto: parte del nivel de enfoque con que acabó (sin
     * saltos) y se adelanta si hay un toque posterior. Tras [PerformanceConfig.awakeSeconds] sin tocar, vuelve a atenuarse.
     */
    private fun postEffectCycleStart(end: Instant, t0: Instant): Instant {
        val base = end - (effectFocusAt(end, t0) * WAKE_MS).toLong().milliseconds
        val t = touchedAt
        return if (t != null && t > base) t else base
    }

    /**
     * Toque sobre la pantalla atenuada: la despierta (o prolonga el tiempo despierta). Mantiene la continuidad si el
     * toque llega a mitad de un fundido. No hace nada si la pantalla no está en modo atenuado.
     */
    fun touch(now: Instant): Performance {
        if (!dimmed) return this
        return copy(touchedAt = now - (focusAt(now) * WAKE_MS).toLong().milliseconds)
    }

    /**
     * Enfoque/brillo de la pantalla simulada (0 = apagada o atenuada del todo, 1 = normal). Con [AodStyle.DIMMED]:
     * esperando el disparo, solo cuentan los toques; mientras corre el efecto, despierta; y al terminar vuelve a
     * atenuarse pasados [PerformanceConfig.awakeSeconds] sin tocar.
     */
    fun focusAt(now: Instant): Float {
        if (!dimmed) return wakeFractionAt(now)
        val t0 = triggeredAt ?: return touchFocusAt(now)
        val end = effectEnd()!!
        if (now < end) return effectFocusAt(now, t0)
        return cycleFocus((now - postEffectCycleStart(end, t0)).inWholeMilliseconds)
    }

    /** Milisegundos hasta que la pantalla despierta empiece a atenuarse (null si no aplica o ya está atenuándose). */
    fun msUntilDimStarts(now: Instant): Long? {
        if (!dimmed) return null
        val t0 = triggeredAt
        val start: Instant = if (t0 == null) {
            touchedAt ?: return null
        } else {
            val end = effectEnd()!!
            if (now < end) return null
            postEffectCycleStart(end, t0)
        }
        val holdEnd = WAKE_MS + config.awakeSeconds * 1000L
        val elapsed = (now - start).inWholeMilliseconds
        return if (elapsed < holdEnd) (holdEnd - elapsed).coerceAtLeast(0L) else null
    }

    fun phaseAt(now: Instant): Phase {
        val t0 = triggeredAt ?: return Phase.ARMED
        val elapsed = (now - t0).inWholeMilliseconds
        return when {
            elapsed < config.delayMs -> Phase.DELAY
            elapsed < config.delayMs + animationMs -> Phase.ANIMATING
            else -> Phase.SYNCED_HOLD
        }
    }

    /**
     * Offset vigente en minutos. Durante la animación baja un paso cada [stepMs] hacia 0 y se suma a la hora real
     * vigente en cada instante: si el minuto real cambia a mitad, el destino se recalcula y termina siempre en la real.
     */
    fun offsetAt(now: Instant): Int = when (phaseAt(now)) {
        Phase.ARMED, Phase.DELAY -> startOffset
        Phase.SYNCED_HOLD -> 0
        Phase.ANIMATING -> {
            val intoAnim = (now - triggeredAt!!).inWholeMilliseconds - config.delayMs
            val steps = (intoAnim / stepMs).toInt()
            if (startOffset > 0) (startOffset - steps).coerceAtLeast(0) else (startOffset + steps).coerceAtMost(0)
        }
    }

    /**
     * Cuánto está "encendida" la pantalla simulada (0 = apagada, 1 = encendida del todo) para animar el encendido
     * con un fundido. Sin Always On siempre vale 1.
     */
    fun wakeFractionAt(now: Instant): Float {
        if (!config.alwaysOn) return 1f
        val t0 = triggeredAt ?: return 0f
        val wakeStart = if (config.autoWake) 0L else config.delayMs
        val elapsed = (now - t0).inWholeMilliseconds - wakeStart
        return (elapsed.toFloat() / WAKE_MS).coerceIn(0f, 1f)
    }

    /** Instante (a minuto completo) que muestra la pantalla falsa. */
    fun displayAt(now: Instant): Instant = displayTime(now, offsetAt(now))

    companion object {
        const val MAX_ANIMATION_MS = 20_000L

        /** Duración del fundido de encendido de la pantalla. */
        const val WAKE_MS = 400L

        /** Duración del fundido de vuelta a la atenuación. */
        const val DIM_MS = 600L
    }
}
