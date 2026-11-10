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

    /** Vuelve a la espera (2 dedos arriba): el offset se recalcula y se cancela cualquier animación. */
    fun reset(): Performance = copy(triggeredAt = null, touchedAt = null)

    private val dimmed: Boolean get() = config.alwaysOn && config.aodStyle == AodStyle.DIMMED

    /** Enfoque debido solo al último toque: sube al tocar, se mantiene [PerformanceConfig.awakeSeconds] y vuelve a bajar. */
    private fun touchFocusAt(now: Instant): Float {
        val t = touchedAt ?: return 0f
        val elapsed = (now - t).inWholeMilliseconds
        val hold = config.awakeSeconds * 1000L
        return when {
            elapsed < 0 -> 0f
            elapsed < WAKE_MS -> elapsed.toFloat() / WAKE_MS
            elapsed < WAKE_MS + hold -> 1f
            else -> (1f - (elapsed - WAKE_MS - hold).toFloat() / DIM_MS).coerceIn(0f, 1f)
        }
    }

    /**
     * Toque sobre la pantalla atenuada: la despierta (o prolonga el tiempo despierta). Mantiene la continuidad si el
     * toque llega a mitad de un fundido. No hace nada si la pantalla no está en modo atenuado.
     */
    fun touch(now: Instant): Performance {
        if (!dimmed) return this
        val f = touchFocusAt(now)
        return copy(touchedAt = now - (f * WAKE_MS).toLong().milliseconds)
    }

    /**
     * Enfoque/brillo de la pantalla simulada (0 = apagada o atenuada del todo, 1 = normal). Con [AodStyle.DIMMED]
     * se combina el despertar por disparo ([wakeFractionAt]) con el despertar por toque.
     */
    fun focusAt(now: Instant): Float {
        val fromTrigger = wakeFractionAt(now)
        return if (dimmed) max(fromTrigger, touchFocusAt(now)) else fromTrigger
    }

    /** Milisegundos hasta que la pantalla despierta por un toque empiece a atenuarse otra vez (null si no aplica). */
    fun msUntilDimStarts(now: Instant): Long? {
        val t = touchedAt ?: return null
        val end = WAKE_MS + config.awakeSeconds * 1000L
        val elapsed = (now - t).inWholeMilliseconds
        return if (elapsed < end) (end - elapsed).coerceAtLeast(0L) else null
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
