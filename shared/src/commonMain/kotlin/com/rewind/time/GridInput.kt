package com.rewind.time

import kotlinx.datetime.Instant

/**
 * Entrada de N (minutos extra) por pulsaciones en una casilla de la pantalla (SPEC CU-06): se cuentan las pulsaciones
 * y, cuando pasa [CONFIRM_MS] sin pulsar, el número contado queda fijado. Es una función pura de (estado, ahora).
 */
data class GridInput(val count: Int = 0, val lastTapAt: Instant? = null) {
    /** Suma una pulsación. */
    fun tap(now: Instant): GridInput = copy(count = (count + 1).coerceAtMost(MAX), lastTapAt = now)

    /** Milisegundos que faltan para confirmar (0 si ya toca confirmar o no hay pulsaciones). */
    fun remainingMs(now: Instant): Long {
        val last = lastTapAt ?: return 0L
        return (CONFIRM_MS - (now - last).inWholeMilliseconds).coerceAtLeast(0L)
    }

    companion object {
        /** Pausa sin pulsar que confirma el número. */
        const val CONFIRM_MS = 1_500L

        /** Máximo de minutos extra (el mismo que en Ajustes). */
        const val MAX = 999
    }
}
