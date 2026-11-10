package com.rewind.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rewind.time.Performance
import com.rewind.time.Phase
import com.rewind.time.TimeSource
import com.rewind.time.formatDate
import com.rewind.time.formatTime
import com.rewind.time.local
import com.rewind.time.millisToNextMinute
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone

private const val KEY_A = "screenshot_a"
private const val KEY_B = "screenshot_b"

private enum class Screen { PERFORMANCE, SETTINGS, DISPLAY }

@Composable
fun RewindRoot(store: SettingsStore, images: ImageStore) {
    var settings by remember { mutableStateOf(store.load()) }
    var screen by remember { mutableStateOf(Screen.PERFORMANCE) }
    var imageA by remember { mutableStateOf(images.load(KEY_A)?.let { decodeImage(it) }) }
    var imageB by remember { mutableStateOf(images.load(KEY_B)?.let { decodeImage(it) }) }
    MaterialTheme(colorScheme = darkColorScheme()) {
        when (screen) {
            Screen.SETTINGS -> SettingsScreen(
                initial = settings,
                onSave = { settings = it; store.save(it); screen = Screen.PERFORMANCE },
                onClose = { screen = Screen.PERFORMANCE },
                hasScreenshot = imageA != null,
                hasDragged = imageB != null,
                onScreenshotPicked = { bytes -> decodeImage(bytes)?.let { images.save(KEY_A, bytes); imageA = it } },
                onDraggedPicked = { bytes -> decodeImage(bytes)?.let { images.save(KEY_B, bytes); imageB = it } },
                onClearImages = { images.delete(KEY_A); images.delete(KEY_B); imageA = null; imageB = null },
                // Se guardan los ajustes pendientes antes de salir al editor para no perderlos.
                onOpenDisplayEditor = { pending -> settings = pending; store.save(pending); screen = Screen.DISPLAY },
            )
            Screen.DISPLAY -> {
                val a = imageA
                if (a == null) screen = Screen.SETTINGS else DisplayEditorScreen(
                    imageA = a,
                    imageB = imageB,
                    initial = settings.display,
                    timeFormat = settings.timeFormat,
                    onSave = { settings = settings.copy(display = it); store.save(settings); screen = Screen.SETTINGS },
                    onClose = { screen = Screen.SETTINGS },
                )
            }
            Screen.PERFORMANCE -> PerformanceScreen(
                settings, screenshot = imageA, dragged = imageB, onOpenSettings = { screen = Screen.SETTINGS },
            )
        }
    }
}

/**
 * Pantalla principal: réplica de la pantalla de bloqueo con el reloj falso (hora real + offset). Un toque dispara
 * el efecto (solo en ARMED); el tick se programa con el reloj del sistema (sin derivar) y por fotogramas mientras
 * hay retardo o animación.
 */
@Composable
fun PerformanceScreen(
    settings: AppSettings,
    screenshot: ImageBitmap?,
    dragged: ImageBitmap?,
    onOpenSettings: () -> Unit,
    source: TimeSource = TimeSource { Clock.System.now() },
) {
    SystemBarsEffect(dark = true)
    var performance by remember(settings) { mutableStateOf(Performance(settings.toConfig())) }
    var timeText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }
    LaunchedEffect(performance, settings) {
        while (true) {
            val now = source.now()
            val shown = performance.displayAt(now).local(TimeZone.currentSystemDefault())
            timeText = formatTime(shown, settings.timeFormat)
            dateText = formatDate(shown, settings.display.dateFormat) // la fecha también cambia al cruzar medianoche
            when (performance.phaseAt(now)) {
                Phase.ANIMATING -> withFrameMillis { }
                Phase.DELAY -> delay(minOf(16L, millisToNextMinute(now)))
                else -> delay(millisToNextMinute(now))
            }
        }
    }
    val gestures = Modifier.pointerInput(Unit) {
        detectRewindGestures(
            onTap = { performance = performance.trigger(source.now()) },
            onTwoFingerSwipe = { up -> if (up) performance = performance.reset() else onOpenSettings() },
            // Alternativa para el emulador: pulsación larga arriba a la derecha = ajustes, arriba a la izquierda = reset.
            onLongPress = { col, row ->
                if (row == 0 && col == 2) onOpenSettings()
                else if (row == 0 && col == 0) performance = performance.reset()
            },
        )
    }
    if (screenshot == null) {
        // Sin captura no se puede actuar (CU-01): se muestran instrucciones.
        Box(Modifier.fillMaxSize().background(Color.Black).then(gestures)) {
            Text(
                "Sin pantalla de bloqueo.\nAjustes (pulsación larga arriba a la derecha) → elige la captura.",
                color = Color.White, textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
        }
    } else {
        LockScreenView(
            screenshot, dragged, settings.display, timeText, dateText,
            modifier = Modifier.fillMaxSize().background(Color.Black).then(gestures),
        )
    }
}
