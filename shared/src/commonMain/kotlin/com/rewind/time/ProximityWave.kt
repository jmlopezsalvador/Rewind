package com.rewind.time

/**
 * "Pasar la mano" por el sensor de proximidad: el gesto se completa al RETIRAR la mano, no al ponerla. Con la mano
 * encima la pantalla está apagada (el sistema la apaga) y todavía no pasa nada; el efecto empieza al destapar.
 *
 * Secuencia que dispara: lejos → cerca → lejos. Hace falta haber visto el sensor DESTAPADO antes de tapar, para no
 * disparar si el móvil arranca tapado (en un bolsillo) o si el estado inicial leído es falso.
 *
 * Es una máquina de estados mínima, sin tiempo ni plataforma, que usan Android e iOS por igual.
 */
class ProximityWave {
    private var sawFar = false
    private var covered = false

    /** Alimentar con cada lectura (true = cerca/tapado). Devuelve true cuando el gesto se completa (al destapar). */
    fun update(near: Boolean): Boolean {
        if (near) {
            if (sawFar) covered = true
            return false
        }
        val completed = covered
        covered = false
        sawFar = true
        return completed
    }
}
