package com.rewind.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.rewind.time.Performance
import com.rewind.time.Phase
import com.rewind.time.TimeSource
import com.rewind.time.formatTime
import com.rewind.time.local
import com.rewind.time.millisToNextMinute
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone

@Composable
fun RewindRoot(store: SettingsStore) {
    val settings by remember { mutableStateOf(store.load()) }
    MaterialTheme {
        PerformanceScreen(settings)
    }
}

/**
 * Pantalla principal (provisional): reloj falso hora real + offset. Un toque dispara el efecto (solo en ARMED);
 * el tick se programa con el reloj del sistema (sin derivar) y por fotogramas mientras hay retardo/animación.
 */
@Composable
fun PerformanceScreen(settings: AppSettings, source: TimeSource = TimeSource { Clock.System.now() }) {
    SystemBarsEffect(dark = true)
    var performance by remember(settings) { mutableStateOf(Performance(settings.toConfig())) }
    var shown by remember { mutableStateOf("") }
    LaunchedEffect(performance) {
        while (true) {
            val now = source.now()
            shown = formatTime(performance.displayAt(now).local(TimeZone.currentSystemDefault()), settings.timeFormat)
            when (performance.phaseAt(now)) {
                Phase.ANIMATING -> withFrameMillis { }
                Phase.DELAY -> delay(minOf(16L, millisToNextMinute(now)))
                else -> delay(millisToNextMinute(now))
            }
        }
    }
    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .pointerInput(Unit) { detectTapGestures { performance = performance.trigger(source.now()) } },
        contentAlignment = Alignment.Center,
    ) {
        Text(shown, color = Color.White, fontSize = 96.sp)
    }
}
