package com.rewind.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.max

/**
 * Gestos de la Performance Screen (SPEC 3 y CU-11):
 * - un toque (un solo dedo, corto y sin moverse) → [onTap] con la casilla (columna, fila) de la pantalla en 3×3;
 * - deslizar con dos dedos hacia abajo/arriba → [onTwoFingerSwipe] (true = hacia arriba);
 * - pulsación larga con un dedo → [onLongPress] con la casilla (columna, fila) de la pantalla dividida en 3×3
 *   (0,0 = arriba izquierda; 2,0 = arriba derecha). Alternativa a los dos dedos para trabajar en el emulador.
 * Un gesto de dos dedos o una pulsación larga nunca cuentan como toque, así no disparan el efecto.
 */
suspend fun PointerInputScope.detectRewindGestures(
    onTap: (col: Int, row: Int) -> Unit,
    onTwoFingerSwipe: (up: Boolean) -> Unit,
    onLongPress: (col: Int, row: Int) -> Unit = { _, _ -> },
) {
    val swipeThreshold = 64.dp.toPx()
    val tapSlop = 24.dp.toPx()
    val tapMaxMs = 500L
    val longPressMs = 600L
    fun cellCol(x: Float) = (x / (size.width / 3f)).toInt().coerceIn(0, 2)
    fun cellRow(y: Float) = (y / (size.height / 3f)).toInt().coerceIn(0, 2)
    awaitEachGesture {
        val first = awaitFirstDown(requireUnconsumed = false)
        val startPos = first.position
        val startTime = first.uptimeMillis
        var maxPointers = 1
        var startY: Float? = null
        var fired = false
        var moved = false
        var longFired = false
        var lastTime = startTime
        do {
            val waitingLong = !longFired && maxPointers == 1 && !moved
            val event = if (waitingLong) {
                withTimeoutOrNull(max(1L, longPressMs - (lastTime - startTime))) {
                    awaitPointerEvent(PointerEventPass.Initial)
                }
            } else {
                awaitPointerEvent(PointerEventPass.Initial)
            }
            if (event == null) {
                // Pasó el tiempo con el dedo quieto: pulsación larga.
                longFired = true
                onLongPress(cellCol(startPos.x), cellRow(startPos.y))
                continue
            }
            val pressed = event.changes.filter { it.pressed }
            maxPointers = max(maxPointers, pressed.size)
            lastTime = event.changes.maxOf { it.uptimeMillis }
            if (pressed.size >= 2) {
                val avgY = pressed.map { it.position.y }.average().toFloat()
                val y0 = startY
                if (y0 == null) startY = avgY
                else if (!fired && abs(avgY - y0) > swipeThreshold) {
                    fired = true
                    onTwoFingerSwipe(avgY < y0)
                }
            } else if (pressed.size == 1) {
                val d = pressed[0].position - startPos
                if (abs(d.x) > tapSlop || abs(d.y) > tapSlop) moved = true
            }
            event.changes.forEach { it.consume() }
        } while (event == null || event.changes.any { it.pressed })
        if (maxPointers == 1 && !moved && !longFired && lastTime - startTime <= tapMaxMs) {
            onTap(cellCol(startPos.x), cellRow(startPos.y))
        }
    }
}
