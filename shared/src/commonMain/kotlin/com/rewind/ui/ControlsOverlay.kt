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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rewind.time.Trigger

/**
 * Capa de ayuda (Ajustes → "Ver controles de pantalla"): dibuja la pantalla dividida en 3×3 y rotula la función de
 * cada casilla. No captura toques: los gestos pasan a través y siguen funcionando igual.
 * Las casillas con función van resaltadas. Al añadir una función por casilla, rotularla en [cellInfo].
 */
@Composable
fun ControlsOverlay(
    numberEntryEnabled: Boolean,
    minutes: Int,
    trigger: Trigger,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        for (row in 0..2) {
            Row(Modifier.weight(1f)) {
                for (col in 0..2) {
                    val (title, detail, active) = cellInfo(col, row, numberEntryEnabled, minutes, trigger)
                    Box(
                        Modifier.weight(1f).fillMaxSize()
                            .background(if (active) Color(0x40FFC107) else Color(0x14000000))
                            .border(1.dp, Color.White.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (title.isNotEmpty()) {
                            Column(
                                Modifier.background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(title, color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center)
                                if (detail.isNotEmpty()) {
                                    Text(detail, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class CellInfo(val title: String, val detail: String, val active: Boolean)

private fun cellInfo(col: Int, row: Int, numberEntry: Boolean, minutes: Int, trigger: Trigger): CellInfo = when {
    col == 0 && row == 0 -> CellInfo("Reiniciar", "pulsación larga", true)
    col == 2 && row == 0 -> CellInfo("Ajustes", "pulsación larga", true)
    col == 0 && row == 2 -> if (numberEntry) {
        CellInfo("Registrar número", "pulsación larga y 2 cifras\n(07, 17...)", true)
    } else {
        CellInfo("Registrar número", "desactivado\n(actívalo en Ajustes)", false)
    }
    col == 1 && row == 1 -> CellInfo(
        "Disparo · N = $minutes",
        when (trigger) {
            Trigger.TAP -> "toque en cualquier casilla libre"
            Trigger.TAP_VIBRATE -> "toque en cualquier casilla libre\n(y vibra)"
            Trigger.PROXIMITY -> "sensor de proximidad\n(los toques no disparan)"
            Trigger.VOLUME -> "tecla de volumen\n(los toques no disparan)"
        },
        false,
    )
    else -> CellInfo("", "", false)
}
