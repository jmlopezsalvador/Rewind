package com.rewind.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Página de instrucciones para el usuario (ver [instructions]). */
@Composable
fun InstructionsScreen(onClose: () -> Unit) {
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Instrucciones", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            instructions(platformSupportsVolumeKeys()).forEach { section ->
                Text(
                    section.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 10.dp),
                )
                section.paragraphs.forEach { p ->
                    Text(
                        p,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = if (p.startsWith("• ")) Modifier.padding(start = 8.dp) else Modifier,
                    )
                }
            }
        }
        Button(onClose, Modifier.fillMaxWidth().padding(12.dp)) { Text("Volver") }
    }
}
