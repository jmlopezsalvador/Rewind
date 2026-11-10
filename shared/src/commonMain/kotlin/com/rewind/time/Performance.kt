package com.rewind.time

import kotlinx.datetime.Instant
import kotlin.math.abs
import kotlin.math.min

/** Tiempo por paso de minuto (SPEC 5; valores propuestos, ver Q4). */
enum class AnimationSpeed(val stepMs: Long) { SLOW(1000), REGULAR(500), FAST(200) }

data class PerformanceConfig(
    val direction: Direction = Direction.BACKWARDS,
    val extraMinutes: Int = 3,
    val animate: Boolean = true,
    val delayMs: Long = 4_000,
    val speed: AnimationSpeed = AnimationSpeed.REGULAR,
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

    /** Vuelve a la espera (2 dedos arriba): el offset se recalcula y se cancela cualquier animación. */
    fun reset(): Performance = copy(triggeredAt = null)

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

    /** Instante (a minuto completo) que muestra la pantalla falsa. */
    fun displayAt(now: Instant): Instant = displayTime(now, offsetAt(now))

    companion object {
        const val MAX_ANIMATION_MS = 20_000L
    }
}
