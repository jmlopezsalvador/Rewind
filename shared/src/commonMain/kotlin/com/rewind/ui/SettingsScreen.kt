package com.rewind.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rewind.time.AnimationSpeed
import com.rewind.time.Direction
import com.rewind.time.TimeFormat
import com.rewind.time.Trigger

/** Ajustes de la actuación (SPEC 5, fase 1). Display Settings (capturas, editor, blend) llegará aparte. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    initial: AppSettings,
    onSave: (AppSettings) -> Unit,
    onClose: () -> Unit,
    hasScreenshot: Boolean,
    hasDragged: Boolean,
    onScreenshotPicked: (ByteArray) -> Unit,
    onDraggedPicked: (ByteArray) -> Unit,
    onClearImages: () -> Unit,
    onOpenDisplayEditor: (AppSettings) -> Unit,
) {
    val pickScreenshot = rememberImagePicker(onScreenshotPicked)
    val pickDragged = rememberImagePicker(onDraggedPicked)
    var s by remember { mutableStateOf(initial) }
    var minutesText by remember { mutableStateOf(initial.extraMinutes.toString()) }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)

        Section("Pantalla de bloqueo (las imágenes se guardan al elegirlas)") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(pickScreenshot, Modifier.weight(1f)) {
                    Text(if (hasScreenshot) "Captura A ✓" else "Elegir captura A")
                }
                OutlinedButton(pickDragged, Modifier.weight(1f)) {
                    Text(if (hasDragged) "Sin reloj B ✓" else "Elegir sin reloj B")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ onOpenDisplayEditor(s) }, enabled = hasScreenshot && minutesText.toIntOrNull() in 1..999) {
                    Text("Editar hora, fecha y mezcla")
                }
                if (hasScreenshot || hasDragged) OutlinedButton(onClearImages) { Text("Quitar imágenes") }
            }
        }

        Section("Dirección") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(s.direction == Direction.BACKWARDS, { s = s.copy(direction = Direction.BACKWARDS) }, { Text("Atrás") })
                FilterChip(s.direction == Direction.FORWARDS, { s = s.copy(direction = Direction.FORWARDS) }, { Text("Adelante") })
            }
        }

        Section("Minutos extra (1–999)") {
            OutlinedTextField(
                value = minutesText,
                onValueChange = { t ->
                    val clean = t.filter { it.isDigit() }.take(3)
                    minutesText = clean
                    clean.toIntOrNull()?.let { s = s.copy(extraMinutes = it.coerceIn(1, 999)) }
                },
                singleLine = true,
                isError = minutesText.toIntOrNull() !in 1..999,
            )
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Fijar los minutos pulsando en directo", color = MaterialTheme.colorScheme.onBackground)
                Text(
                    "Pulsa N veces la casilla izquierda-centro de la pantalla y espera 1,5 s. Esa casilla deja de disparar el efecto.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
            }
            Switch(s.gridInput, { s = s.copy(gridInput = it) })
        }

        Section("Formato de hora") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    TimeFormat.H12 to "12 h", TimeFormat.H12_TWO_DIGITS to "12 h (2)",
                    TimeFormat.H24 to "24 h", TimeFormat.H24_TWO_DIGITS to "24 h (2)",
                ).forEach { (f, label) -> FilterChip(s.timeFormat == f, { s = s.copy(timeFormat = f) }, { Text(label) }) }
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Animar el rebobinado", Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground)
            Switch(s.animate, { s = s.copy(animate = it) })
        }

        Section("Retardo antes de animar (${s.delaySeconds} s)") {
            Slider(
                value = s.delaySeconds.toFloat(),
                onValueChange = { s = s.copy(delaySeconds = it.toInt().coerceIn(1, 10)) },
                valueRange = 1f..10f, steps = 8,
            )
        }

        Section("Velocidad de animación") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(AnimationSpeed.SLOW to "Lenta", AnimationSpeed.REGULAR to "Normal", AnimationSpeed.FAST to "Rápida")
                    .forEach { (v, label) -> FilterChip(s.speed == v, { s = s.copy(speed = v) }, { Text(label) }) }
            }
        }

        Section("Disparador") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Trigger.TAP to "Toque", Trigger.TAP_VIBRATE to "Toque + vibración", Trigger.PROXIMITY to "Proximidad")
                    .forEach { (v, label) -> FilterChip(s.trigger == v, { s = s.copy(trigger = v) }, { Text(label) }) }
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Simular pantalla apagada (Always On)", Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground)
            Switch(s.alwaysOn, { s = s.copy(alwaysOn = it) })
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Encender sola al disparar (Auto Wake)", Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (s.alwaysOn) 1f else 0.4f),
            )
            Switch(s.autoWake, { s = s.copy(autoWake = it) }, enabled = s.alwaysOn)
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Alerta de cambio de minuto (brillo en el borde)", Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground)
            Switch(s.minuteAlert, { s = s.copy(minuteAlert = it) })
        }
        if (s.minuteAlert) {
            Section("Avisar cuando falten ${s.alertSeconds} s") {
                Slider(
                    value = s.alertSeconds.toFloat(),
                    onValueChange = { s = s.copy(alertSeconds = it.toInt().coerceIn(3, 30)) },
                    valueRange = 3f..30f, steps = 26,
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClose, Modifier.weight(1f)) { Text("Cerrar") }
            Button(
                onClick = { onSave(s) },
                Modifier.weight(1f),
                enabled = minutesText.toIntOrNull() in 1..999,
            ) { Text("Guardar") }
        }
        Text(
            "Pantalla principal: 2 dedos ↓ (o pulsación larga arriba-derecha) = ajustes · 2 dedos ↑ (o pulsación larga arriba-izquierda) = reiniciar",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
        content()
    }
}
