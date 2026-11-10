package com.rewind.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rewind.time.NumberEntry

/**
 * Teclado visible del modo de registro de número: 3 columnas × 4 filas (1–9 y, abajo, una fila entera para el 0),
 * ocupando toda la pantalla, con la [opacity] elegida en Ajustes. No captura toques: los lee `PerformanceScreen`.
 * Arriba se ve lo tecleado hasta ahora (por ejemplo «1 _»).
 */
@Composable
fun NumberKeypadOverlay(entry: NumberEntry, opacity: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            for (row in 0..2) {
                Row(Modifier.weight(1f)) {
                    for (col in 0..2) Key((row * 3 + col + 1).toString(), opacity, Modifier.weight(1f))
                }
            }
            Row(Modifier.weight(1f)) { Key("0", opacity, Modifier.weight(1f)) }
        }
        val shown = (0 until NumberEntry.DIGITS).joinToString(" ") { entry.digits.getOrNull(it)?.toString() ?: "_" }
        Text(
            shown,
            color = Color.White.copy(alpha = (opacity + 0.3f).coerceAtMost(1f)),
            fontSize = 28.sp,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 56.dp),
        )
    }
}

@Composable
private fun Key(label: String, opacity: Float, modifier: Modifier) {
    Box(
        modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = opacity))
            .border(1.dp, Color.White.copy(alpha = opacity)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White.copy(alpha = (opacity + 0.3f).coerceAtMost(1f)), fontSize = 44.sp)
    }
}
