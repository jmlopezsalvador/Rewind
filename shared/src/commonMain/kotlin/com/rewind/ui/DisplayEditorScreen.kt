package com.rewind.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.rewind.display.BlendConfig
import com.rewind.display.BoxRect
import com.rewind.display.DisplayConfig
import com.rewind.display.TextAlignment
import com.rewind.display.TextLook
import com.rewind.time.DateFormat
import com.rewind.time.formatDate
import com.rewind.time.formatTime
import com.rewind.time.local
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone

private enum class Tab { TIME, DATE, BLEND }

/**
 * Editor de Display (SPEC 4.2 y 4.3): cajas de hora (roja) y fecha (azul) arrastrables y redimensionables
 * sobre la captura A, estilo del texto y mezcla del parche B. Se ve el render real con la hora actual.
 */
@Composable
fun DisplayEditorScreen(
    imageA: ImageBitmap,
    imageB: ImageBitmap?,
    initial: DisplayConfig,
    timeFormat: com.rewind.time.TimeFormat,
    onSave: (DisplayConfig) -> Unit,
    onClose: () -> Unit,
) {
    var cfg by remember { mutableStateOf(initial) }
    var tab by remember { mutableStateOf(Tab.TIME) }
    var showOriginal by remember { mutableStateOf(false) }
    val local = Clock.System.now().local(TimeZone.currentSystemDefault())
    val timeText = formatTime(local, timeFormat)
    val dateText = formatDate(local, cfg.dateFormat)

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding()) {
        Box(Modifier.fillMaxWidth().weight(1f).padding(8.dp), contentAlignment = Alignment.Center) {
            LockScreenView(
                imageA, imageB, cfg, timeText, dateText,
                modifier = Modifier.aspectRatio(imageA.width.toFloat() / imageA.height, matchHeightConstraintsFirst = true),
                showOriginal = showOriginal,
            ) { w, h ->
                BoxesOverlay(w, h, cfg, tab, onSelect = { tab = it }, onChange = { cfg = it })
            }
        }

        Column(
            Modifier.fillMaxWidth().heightIn(max = 280.dp).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(tab == Tab.TIME, { tab = Tab.TIME }, { Text("Hora") })
                FilterChip(tab == Tab.DATE, { tab = Tab.DATE }, { Text("Fecha") })
                FilterChip(tab == Tab.BLEND, { tab = Tab.BLEND }, { Text("Mezcla") })
                Text("Ver A", Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground)
                Switch(showOriginal, { showOriginal = it })
            }
            when (tab) {
                Tab.TIME -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Reloj apilado (2 líneas)", Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground)
                        Switch(cfg.timeStacked, { cfg = cfg.withStacked(it) })
                    }
                    LookControls(cfg.timeLook) { cfg = cfg.copy(timeLook = it) }
                }
                Tab.DATE -> {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DateFormat.entries.forEach { f ->
                            FilterChip(cfg.dateFormat == f, { cfg = cfg.copy(dateFormat = f) }, { Text(f.label) })
                        }
                    }
                    LookControls(cfg.dateLook) { cfg = cfg.copy(dateLook = it) }
                }
                Tab.BLEND -> BlendControls(cfg.blend, imageB != null) { cfg = cfg.copy(blend = it) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClose, Modifier.weight(1f)) { Text("Cerrar") }
            Button({ onSave(cfg) }, Modifier.weight(1f)) { Text("Guardar") }
        }
    }
}

/** Cajas discontinuas (roja = hora, azul = fecha) con asa de redimensionado abajo a la derecha. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.BoxesOverlay(
    w: Float, h: Float, cfg: DisplayConfig, selected: Tab,
    onSelect: (Tab) -> Unit, onChange: (DisplayConfig) -> Unit,
) {
    val cfgNow = rememberUpdatedState(cfg)
    val selNow = rememberUpdatedState(selected)
    // Qué se está arrastrando: (caja, true = redimensionar)
    var dragging by remember { mutableStateOf<Pair<Tab, Boolean>?>(null) }

    Box(
        Modifier.fillMaxSize().pointerInput(w, h) {
            val handle = 28.dp.toPx()
            detectDragGestures(
                onDragStart = { p ->
                    val c = cfgNow.value
                    fun hitHandle(r: BoxRect) =
                        kotlin.math.abs(p.x - (r.x + r.w) * w) < handle && kotlin.math.abs(p.y - (r.y + r.h) * h) < handle
                    fun inside(r: BoxRect) = p.x in r.x * w..(r.x + r.w) * w && p.y in r.y * h..(r.y + r.h) * h
                    val order = if (selNow.value == Tab.DATE) listOf(Tab.DATE, Tab.TIME) else listOf(Tab.TIME, Tab.DATE)
                    dragging = null
                    for (t in order) {
                        val r = if (t == Tab.TIME) c.timeBox else c.dateBox
                        if (hitHandle(r)) { dragging = t to true; onSelect(t); break }
                    }
                    if (dragging == null) for (t in order) {
                        val r = if (t == Tab.TIME) c.timeBox else c.dateBox
                        if (inside(r)) { dragging = t to false; onSelect(t); break }
                    }
                },
                onDragEnd = { dragging = null },
                onDragCancel = { dragging = null },
            ) { change, drag ->
                val (t, resize) = dragging ?: return@detectDragGestures
                change.consume()
                val c = cfgNow.value
                val r = if (t == Tab.TIME) c.timeBox else c.dateBox
                val dx = drag.x / w
                val dy = drag.y / h
                val nr = (if (resize) r.copy(w = r.w + dx, h = r.h + dy) else r.copy(x = r.x + dx, y = r.y + dy)).clamped()
                onChange(if (t == Tab.TIME) c.copy(timeBox = nr) else c.copy(dateBox = nr))
            }
        },
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))
            fun draw(r: BoxRect, color: Color, isSel: Boolean) {
                val tl = Offset(r.x * size.width, r.y * size.height)
                val sz = Size(r.w * size.width, r.h * size.height)
                drawRect(color, tl, sz, style = Stroke(width = if (isSel) 5f else 3f, pathEffect = dash))
                drawRect(color, Offset(tl.x + sz.width - 14f, tl.y + sz.height - 14f), Size(28f, 28f))
            }
            draw(cfg.dateBox, Color(0xFF3D8BFF), selected == Tab.DATE)
            draw(cfg.timeBox, Color(0xFFFF3B3B), selected == Tab.TIME)
        }
    }
}

@Composable
private fun LookControls(look: TextLook, onChange: (TextLook) -> Unit) {
    LabeledSlider("Tamaño", look.size, 0.008f, 0.30f) { onChange(look.copy(size = it)) }
    LabeledSlider("Grosor (${look.weight})", look.weight / 100f, 1f, 9f, steps = 7) { onChange(look.copy(weight = (it.toInt() * 100).coerceIn(100, 900))) }
    LabeledSlider("Ancho / espaciado", look.spacing, -0.1f, 0.3f) { onChange(look.copy(spacing = it)) }
    LabeledSlider("Opacidad", look.opacity, 0.1f, 1f) { onChange(look.copy(opacity = it)) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(look.align == TextAlignment.START, { onChange(look.copy(align = TextAlignment.START)) }, { Text("Izq.") })
        FilterChip(look.align == TextAlignment.CENTER, { onChange(look.copy(align = TextAlignment.CENTER)) }, { Text("Centro") })
        FilterChip(look.align == TextAlignment.END, { onChange(look.copy(align = TextAlignment.END)) }, { Text("Der.") })
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0xFFFFFF, 0xE6E6E6, 0xB0B0B0, 0x808080, 0x404040, 0x000000).forEach { c ->
            Box(
                Modifier.size(34.dp).background(Color(0xFF000000.toInt() or c), CircleShape)
                    .border(if (look.color == c) 3.dp else 1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { onChange(look.copy(color = c)) },
            )
        }
    }
    val r = (look.color shr 16) and 0xFF
    val g = (look.color shr 8) and 0xFF
    val b = look.color and 0xFF
    fun rgb(nr: Int, ng: Int, nb: Int) = onChange(look.copy(color = (nr shl 16) or (ng shl 8) or nb))
    LabeledSlider("Rojo", r.toFloat(), 0f, 255f) { rgb(it.toInt(), g, b) }
    LabeledSlider("Verde", g.toFloat(), 0f, 255f) { rgb(r, it.toInt(), b) }
    LabeledSlider("Azul", b.toFloat(), 0f, 255f) { rgb(r, g, it.toInt()) }
}

@Composable
private fun BlendControls(blend: BlendConfig, hasB: Boolean, onChange: (BlendConfig) -> Unit) {
    Text(
        if (hasB) "Ajusta hasta que el parche de la captura B se funda con A sin costuras."
        else "Sin captura B no hay parche que mezclar (elígela en Ajustes).",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground,
    )
    LabeledSlider("Brillo", blend.brightness, -0.5f, 0.5f) { onChange(blend.copy(brightness = it)) }
    LabeledSlider("Contraste", blend.contrast, 0.5f, 1.5f) { onChange(blend.copy(contrast = it)) }
    LabeledSlider("Saturación", blend.saturation, 0f, 2f) { onChange(blend.copy(saturation = it)) }
    LabeledSlider("General (opacidad)", blend.overall, 0f, 1f) { onChange(blend.copy(overall = it)) }
    OutlinedButton({ onChange(BlendConfig()) }) { Text("Restablecer mezcla") }
}

@Composable
private fun LabeledSlider(
    label: String, value: Float, min: Float, max: Float, steps: Int = 0, onChange: (Float) -> Unit,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground)
        Slider(value.coerceIn(min, max), onChange, valueRange = min..max, steps = steps)
    }
}
