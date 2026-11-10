package com.rewind.time

import kotlinx.datetime.Instant

/**
 * Modo de registro de número (cuadrícula 3×3 + 0): se entra con una pulsación larga en la casilla inferior izquierda
 * y se teclean exactamente dos cifras (07, 17...). Al teclear la segunda cifra el modo se cierra y el valor queda registrado.
 * Es una función pura de (estado, ahora); el modo se cierra solo tras [TIMEOUT_MS] sin pulsar (seguro anti-bloqueo).
 */
data class NumberEntry(val digits: List<Int> = emptyList(), val lastInputAt: Instant) {
    /** Añade una cifra (0..9). */
    fun press(digit: Int, now: Instant): NumberEntry = copy(digits = digits + digit.coerceIn(0, 9), lastInputAt = now)

    val isComplete: Boolean get() = digits.size >= DIGITS

    /** Valor de las dos cifras (00..99); solo tiene sentido si [isComplete]. */
    val value: Int get() = digits.take(DIGITS).fold(0) { acc, d -> acc * 10 + d }

    /** Milisegundos hasta que el modo se cierre solo por inactividad (0 si ya toca). */
    fun remainingMs(now: Instant): Long =
        (TIMEOUT_MS - (now - lastInputAt).inWholeMilliseconds).coerceAtLeast(0L)

    companion object {
        const val DIGITS = 2

        /** Inactividad tras la que el modo se cierra solo, para no dejar la pantalla bloqueada si se entra por error. */
        const val TIMEOUT_MS = 10_000L
    }
}

/**
 * Cifra que corresponde a un punto de la pantalla (fracciones 0..1) en el teclado: 3 columnas, 4 filas.
 * Las tres primeras filas son 1–9 y la fila de abajo entera es el 0 (zona grande, fácil de acertar a ciegas).
 */
fun keypadDigitAt(fx: Float, fy: Float): Int {
    val col = (fx * 3).toInt().coerceIn(0, 2)
    val row = (fy * 4).toInt().coerceIn(0, 3)
    return if (row == 3) 0 else row * 3 + col + 1
}
