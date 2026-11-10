package com.rewind.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rewind.time.NumberEntry
import com.rewind.time.Performance
import com.rewind.time.Phase
import com.rewind.time.TimeSource
import com.rewind.time.Trigger
import com.rewind.time.formatDate
import com.rewind.time.formatTime
import com.rewind.time.isMinuteAlertActive
import com.rewind.time.keypadDigitAt
import com.rewind.time.local
import com.rewind.time.millisToMinuteAlert
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
 * Pantalla principal: réplica de la pantalla de bloqueo con el reloj falso (hora real + offset).
 * - Disparo (solo en ARMED): toque, toque con vibración, proximidad o tecla de volumen (según los ajustes).
 * - Always On: la pantalla parece apagada hasta el disparo; Auto Wake la "enciende" con un fundido.
 * - Alerta de minuto: brillo sutil abajo cuando falta poco para el cambio de minuto real.
 * - Registro de número: pulsación larga abajo a la izquierda → teclado 3×3 + 0, dos cifras, vibra al entrar y al registrar.
 *   Mientras dura, el resto de la pantalla no responde.
 * - El tick se programa con el reloj del sistema (sin derivar) y por fotogramas durante retardo/animación.
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
    var wake by remember { mutableFloatStateOf(if (settings.alwaysOn) 0f else 1f) }
    var alertOn by remember { mutableStateOf(false) }
    var entry by remember(settings) { mutableStateOf<NumberEntry?>(null) } // modo de registro de número

    val vibrate = rememberVibrate()
    val fire = rememberUpdatedState {
        // el disparo solo cuenta en ARMED y fuera del modo de registro
        if (performance.triggeredAt == null && entry == null) {
            performance = performance.trigger(source.now())
            if (settings.trigger == Trigger.TAP_VIBRATE) vibrate()
        }
    }
    // El sensor solo está registrado mientras se espera el disparo (batería).
    ProximityEffect(
        enabled = settings.trigger == Trigger.PROXIMITY && performance.triggeredAt == null,
        onNear = { fire.value() },
    )
    // Con el disparador de volumen, las teclas no tocan el volumen del sistema mientras se está en esta pantalla.
    VolumeKeyEffect(enabled = settings.trigger == Trigger.VOLUME, onPress = { fire.value() })

    // El modo de registro se cierra solo tras unos segundos sin pulsar (seguro por si se entra sin querer).
    LaunchedEffect(entry) {
        val e = entry
        if (e != null) {
            delay(e.remainingMs(source.now()))
            entry = null
        }
    }

    LaunchedEffect(performance, settings) {
        while (true) {
            val now = source.now()
            val shown = performance.displayAt(now).local(TimeZone.currentSystemDefault())
            timeText = formatTime(shown, settings.timeFormat)
            dateText = formatDate(shown, settings.display.dateFormat) // la fecha también cambia al cruzar medianoche
            wake = performance.wakeFractionAt(now)
            alertOn = settings.minuteAlert && isMinuteAlertActive(now, settings.alertSeconds)
            when (performance.phaseAt(now)) {
                Phase.ANIMATING -> withFrameMillis { }
                Phase.DELAY -> delay(minOf(16L, millisToNextMinute(now)))
                else -> {
                    var wait = millisToNextMinute(now)
                    if (settings.minuteAlert && !alertOn) wait = minOf(wait, millisToMinuteAlert(now, settings.alertSeconds).coerceAtLeast(1L))
                    // con la pantalla "encendiéndose" hace falta animar fotogramas
                    delay(if (wake in 0.001f..0.999f) 16L else wait)
                }
            }
        }
    }

    // Reiniciar vuelve al N de Ajustes (se descarta el registrado en directo) y cierra el modo de registro.
    val resetAll = { performance = Performance(settings.toConfig()); entry = null }
    val gestures = Modifier.pointerInput(Unit) {
        detectRewindGestures(
            onTap = { fx, fy ->
                val e = entry
                if (e != null) {
                    // Modo de registro: solo responde el teclado. Tras la segunda cifra se cierra, se registra y vibra.
                    val next = e.press(keypadDigitAt(fx, fy), source.now())
                    if (next.isComplete) {
                        entry = null
                        if (next.value > 0) performance = performance.withMinutes(next.value) // 00 = sin cambio
                        vibrate()
                    } else {
                        entry = next
                    }
                } else if (settings.trigger == Trigger.TAP || settings.trigger == Trigger.TAP_VIBRATE) {
                    fire.value()
                }
            },
            onTwoFingerSwipe = { up -> if (entry == null) { if (up) resetAll() else onOpenSettings() } },
            // Pulsación larga: abajo-izquierda entra en el registro de número; arriba-derecha, ajustes; arriba-izquierda, reiniciar.
            onLongPress = { fx, fy ->
                val col = cellOf(fx)
                val row = cellOf(fy)
                if (entry == null) {
                    if (settings.numberEntry && col == 0 && row == 2 && performance.triggeredAt == null) {
                        entry = NumberEntry(lastInputAt = source.now())
                        vibrate() // avisa de que el modo de registro está activo
                    } else if (row == 0 && col == 2) {
                        onOpenSettings()
                    } else if (row == 0 && col == 0) {
                        resetAll()
                    }
                }
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
            if (settings.showControls) {
                ControlsOverlay(settings.numberEntry, performance.config.extraMinutes, settings.trigger)
            }
        }
    } else {
        val glow = remember(screenshot) { if (bottomLuminance(screenshot) > 0.5f) Color.Black else Color.White }
        Box(Modifier.fillMaxSize().background(Color.Black).then(gestures)) {
            if (wake > 0f) {
                LockScreenView(
                    screenshot, dragged, settings.display, timeText, dateText,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        // fundido y escalado leve, como el encendido de la pantalla del sistema (CU-09)
                        alpha = wake
                        val sc = 0.97f + 0.03f * wake
                        scaleX = sc; scaleY = sc
                    },
                )
            }
            if (alertOn) {
                Box(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(72.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, glow.copy(alpha = 0.35f)))),
                )
            }
            if (settings.showControls) {
                ControlsOverlay(settings.numberEntry, performance.config.extraMinutes, settings.trigger)
            }
            val e = entry
            if (e != null && settings.keypadVisible) {
                NumberKeypadOverlay(e, settings.keypadOpacity)
            }
        }
    }
}

/** Luminosidad media (0..1) de la franja inferior de la imagen, para elegir un brillo de alerta que contraste. */
private fun bottomLuminance(image: ImageBitmap): Float {
    val w = image.width
    val h = maxOf(1, image.height / 20)
    val pixels = IntArray(w * h)
    image.readPixels(pixels, startX = 0, startY = image.height - h, width = w, height = h)
    var sum = 0.0
    for (p in pixels) {
        val r = (p shr 16) and 0xFF
        val g = (p shr 8) and 0xFF
        val b = p and 0xFF
        sum += 0.299 * r + 0.587 * g + 0.114 * b
    }
    return (sum / pixels.size / 255.0).toFloat()
}
