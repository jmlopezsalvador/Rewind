package com.rewind.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
 * Teclado visible del modo de registro de número: ocupa solo la MITAD INFERIOR de la pantalla, en 3 columnas × 4 filas
 * (1–9 y, abajo, una fila entera para el 0), con la [opacity] elegida en Ajustes. La mitad de arriba no responde.
 * No captura toques: los lee `PerformanceScreen` (ver `keypadDigitAt`). Justo encima del teclado se ve lo tecleado
 * hasta ahora (por ejemplo «1 _»).
 */
@Composable
fun NumberKeypadOverlay(entry: NumberEntry, opacity: Float, modifier: Modifier = Modifier) {
    val textColor = Color.White.copy(alpha = (opacity + 0.3f).coerceAtMost(1f))
    Column(modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
            val shown = (0 until NumberEntry.DIGITS).joinToString(" ") { entry.digits.getOrNull(it)?.toString() ?: "_" }
            Text(shown, color = textColor, fontSize = 28.sp, modifier = Modifier.padding(bottom = 12.dp))
        }
        Column(Modifier.weight(1f)) {
            for (row in 0..2) {
                Row(Modifier.weight(1f)) {
                    for (col in 0..2) Key((row * 3 + col + 1).toString(), opacity, Modifier.weight(1f))
                }
            }
            Row(Modifier.weight(1f)) { Key("0", opacity, Modifier.weight(1f)) }
        }
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
        Text(label, color = Color.White.copy(alpha = (opacity + 0.3f).coerceAtMost(1f)), fontSize = 34.sp)
    }
}
