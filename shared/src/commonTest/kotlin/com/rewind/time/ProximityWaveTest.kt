package com.rewind.time

import kotlin.test.Test
import kotlin.test.assertEquals

class ProximityWaveTest {
    /** Alimenta una secuencia (true = cerca) y devuelve en qué lecturas se completó el gesto. */
    private fun run(vararg readings: Boolean): List<Int> {
        val w = ProximityWave()
        return readings.indices.filter { w.update(readings[it]) }
    }

    @Test fun firesWhenTheHandIsRemovedNotWhenItIsPlaced() {
        // lejos, CERCA (aún nada), lejos (aquí dispara)
        assertEquals(listOf(2), run(false, true, false))
    }

    @Test fun repeatedReadingsFireOnlyOnce() {
        assertEquals(listOf(5), run(false, false, true, true, true, false, false, false))
    }

    @Test fun doesNotFireWithoutHavingSeenTheSensorUncovered() {
        // arranca tapado (bolsillo): al destapar solo se arma, no dispara
        assertEquals(emptyList(), run(true, true, false))
        // y la siguiente pasada sí
        assertEquals(listOf(4), run(true, true, false, true, false))
    }

    @Test fun staleCoveredStartDoesNotFireButTheNextWaveDoes() {
        // estado inicial "pegado" en cerca; luego lejos real; después una pasada completa
        assertEquals(listOf(4), run(true, false, true, true, false))
    }

    @Test fun uncoveredAloneNeverFires() {
        assertEquals(emptyList(), run(false, false, false))
    }

    @Test fun twoWavesFireTwice() {
        assertEquals(listOf(2, 5), run(false, true, false, false, true, false))
    }
}
