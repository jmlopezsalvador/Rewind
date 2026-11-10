package com.rewind.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import com.rewind.display.BlendConfig
import com.rewind.display.BoxRect
import com.rewind.display.DisplayConfig
import com.rewind.display.TextAlignment
import com.rewind.display.TextLook
import com.rewind.display.blendColorMatrix
import kotlin.math.roundToInt

/**
 * Réplica de la pantalla de bloqueo (SPEC 4.1): capas A (base) → parche de B recortado a las cajas de hora y fecha
 * (con la mezcla aplicada) → texto dinámico. Lo usan la Performance Screen y el editor, así se ven idénticas.
 * Con [showOriginal] solo se ve la captura A (para comparar con el render).
 * [overlay] recibe el ancho y el alto en píxeles y se dibuja encima (el editor pinta ahí las cajas).
 */
@Composable
fun LockScreenView(
    imageA: ImageBitmap,
    imageB: ImageBitmap?,
    display: DisplayConfig,
    timeText: String,
    dateText: String,
    modifier: Modifier = Modifier,
    showOriginal: Boolean = false,
    overlay: @Composable BoxScope.(width: Float, height: Float) -> Unit = { _, _ -> },
) {
    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        Canvas(Modifier.fillMaxSize()) {
            drawImage(
                imageA, IntOffset.Zero, IntSize(imageA.width, imageA.height),
                IntOffset.Zero, IntSize(size.width.roundToInt(), size.height.roundToInt()),
                filterQuality = FilterQuality.Medium,
            )
            if (!showOriginal && imageB != null) {
                val filter = ColorFilter.colorMatrix(ColorMatrix(blendColorMatrix(display.blend)))
                listOf(display.timeBox, display.dateBox).forEach { r -> drawPatch(imageB, r, display.blend, filter) }
            }
        }
        if (!showOriginal) {
            BoxText(display.timeBox, display.timeLook, timeText, w, h, stacked = display.timeStacked)
            BoxText(display.dateBox, display.dateLook, dateText, w, h)
        }
        overlay(w, h)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPatch(
    image: ImageBitmap, r: BoxRect, blend: BlendConfig, filter: ColorFilter,
) {
    val sx = (r.x * image.width).roundToInt().coerceIn(0, image.width - 1)
    val sy = (r.y * image.height).roundToInt().coerceIn(0, image.height - 1)
    val sw = (r.w * image.width).roundToInt().coerceIn(1, image.width - sx)
    val sh = (r.h * image.height).roundToInt().coerceIn(1, image.height - sy)
    drawImage(
        image, IntOffset(sx, sy), IntSize(sw, sh),
        IntOffset((r.x * size.width).roundToInt(), (r.y * size.height).roundToInt()),
        IntSize((r.w * size.width).roundToInt().coerceAtLeast(1), (r.h * size.height).roundToInt().coerceAtLeast(1)),
        alpha = blend.overall, colorFilter = filter, filterQuality = FilterQuality.Medium,
    )
}

@Composable
private fun BoxText(rect: BoxRect, look: TextLook, text: String, w: Float, h: Float, stacked: Boolean = false) {
    val density = LocalDensity.current
    val alignment = when (look.align) {
        TextAlignment.START -> Alignment.CenterStart
        TextAlignment.CENTER -> Alignment.Center
        TextAlignment.END -> Alignment.CenterEnd
    }
    Box(
        Modifier
            .offset { IntOffset((rect.x * w).roundToInt(), (rect.y * h).roundToInt()) }
            .size(with(density) { (rect.w * w).toDp() }, with(density) { (rect.h * h).toDp() }),
        contentAlignment = alignment,
    ) {
        // Apilado: "12:42" → "12" encima de "42", con interlineado apretado como en Pixel.
        val shown = if (stacked) text.replace(":", "\n") else text
        val fontPx = look.size * h
        Text(
            shown,
            lineHeight = if (stacked) with(density) { (fontPx * 0.77f).toSp() } else TextUnit.Unspecified,
            textAlign = when (look.align) {
                TextAlignment.START -> TextAlign.Start
                TextAlignment.CENTER -> TextAlign.Center
                TextAlignment.END -> TextAlign.End
            },
            color = Color(0xFF000000.toInt() or look.color).copy(alpha = look.opacity),
            fontSize = with(density) { (look.size * h).toSp() },
            fontWeight = FontWeight(look.weight),
            letterSpacing = look.spacing.em,
            softWrap = false,
            maxLines = if (stacked) 2 else 1,
            modifier = Modifier.wrapContentSize(align = alignment, unbounded = true),
        )
    }
}
