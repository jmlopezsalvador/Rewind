package com.rewind.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max

/**
 * Gestos de la Performance Screen (SPEC 3 y CU-11):
 * - un toque (un solo dedo, corto y sin moverse) → [onTap];
 * - deslizar con dos dedos hacia abajo/arriba → [onTwoFingerSwipe] (true = hacia arriba).
 * Un gesto de dos dedos nunca cuenta como toque, así no dispara el efecto.
 */
suspend fun PointerInputScope.detectRewindGestures(
    onTap: () -> Unit,
    onTwoFingerSwipe: (up: Boolean) -> Unit,
) {
    val swipeThreshold = 64.dp.toPx()
    val tapSlop = 24.dp.toPx()
    val tapMaxMs = 500L
    awaitEachGesture {
        val first = awaitFirstDown(requireUnconsumed = false)
        val startPos = first.position
        val startTime = first.uptimeMillis
        var maxPointers = 1
        var startY: Float? = null
        var fired = false
        var moved = false
        var lastTime = startTime
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
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
        } while (event.changes.any { it.pressed })
        if (maxPointers == 1 && !moved && lastTime - startTime <= tapMaxMs) onTap()
    }
}
