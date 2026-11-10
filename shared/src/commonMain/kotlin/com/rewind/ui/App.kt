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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.rewind.time.TimeSource
import com.rewind.time.displayTime
import com.rewind.time.formatTime
import com.rewind.time.local
import com.rewind.time.millisToNextMinute
import com.rewind.time.offsetMinutes
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

/** Pantalla principal (provisional): reloj falso hora real + offset, sincronizado con el cambio de minuto real. */
@Composable
fun PerformanceScreen(settings: AppSettings, source: TimeSource = TimeSource { Clock.System.now() }) {
    SystemBarsEffect(dark = true)
    var shown by remember { mutableStateOf("") }
    LaunchedEffect(settings) {
        val offset = offsetMinutes(settings.direction, settings.extraMinutes)
        while (true) {
            val now = source.now()
            shown = formatTime(displayTime(now, offset).local(TimeZone.currentSystemDefault()), settings.timeFormat)
            delay(millisToNextMinute(now)) // se recalcula con el reloj del sistema: no deriva
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Text(shown, color = Color.White, fontSize = 96.sp)
    }
}
