package com.rewind.time

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.minutes

/** Sentido del rebobinado. */
enum class Direction { BACKWARDS, FORWARDS }

/** Formatos de hora (SPEC 4.4). En 12 h, medianoche y mediodía se muestran como 12. */
enum class TimeFormat { H12, H12_TWO_DIGITS, H24, H24_TWO_DIGITS }

/** Fuente de tiempo inyectable para poder testear la lógica sin reloj real. */
fun interface TimeSource {
    fun now(): Instant
}

/** Desplazamiento en minutos: Atrás = +N (la pantalla va por delante), Adelante = -N. */
fun offsetMinutes(direction: Direction, n: Int): Int =
    if (direction == Direction.BACKWARDS) n else -n

private fun floorMod(a: Long, b: Long): Long = ((a % b) + b) % b

/** Trunca al minuto (segundo :00). */
fun truncMinute(t: Instant): Instant =
    Instant.fromEpochSeconds(t.epochSeconds - floorMod(t.epochSeconds, 60L))

/** Hora que muestra la pantalla falsa: minuto real truncado + offset. */
fun displayTime(now: Instant, offsetMin: Int): Instant =
    truncMinute(now) + offsetMin.minutes

/** Milisegundos hasta el próximo cambio de minuto real (para programar el tick con el reloj del sistema, sin derivar). */
fun millisToNextMinute(now: Instant): Long =
    60_000L - floorMod(now.toEpochMilliseconds(), 60_000L)

fun Instant.local(zone: TimeZone): LocalDateTime = toLocalDateTime(zone)

fun formatTime(t: LocalDateTime, format: TimeFormat): String {
    val m = t.minute.toString().padStart(2, '0')
    return when (format) {
        TimeFormat.H24 -> "${t.hour}:$m"
        TimeFormat.H24_TWO_DIGITS -> "${t.hour.toString().padStart(2, '0')}:$m"
        TimeFormat.H12, TimeFormat.H12_TWO_DIGITS -> {
            val h = if (t.hour % 12 == 0) 12 else t.hour % 12
            val hs = if (format == TimeFormat.H12_TWO_DIGITS) h.toString().padStart(2, '0') else h.toString()
            "$hs:$m"
        }
    }
}
