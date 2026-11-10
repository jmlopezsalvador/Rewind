package com.rewind.display

import com.rewind.time.DateFormat

/** Rectángulo en fracciones (0..1) del ancho/alto de la pantalla. */
data class BoxRect(val x: Float, val y: Float, val w: Float, val h: Float) {
    /** Mantiene el rectángulo dentro de la pantalla y con un tamaño mínimo. */
    fun clamped(): BoxRect {
        val nw = w.coerceIn(MIN, 1f)
        val nh = h.coerceIn(MIN, 1f)
        return BoxRect(x.coerceIn(0f, 1f - nw), y.coerceIn(0f, 1f - nh), nw, nh)
    }

    companion object { const val MIN = 0.03f }
}

enum class TextAlignment { START, CENTER, END }

/** Aspecto del texto. [size] es fracción del alto de pantalla; [spacing] en em; [color] es RGB (0xRRGGBB). */
data class TextLook(
    val size: Float,
    val weight: Int = 300,
    val spacing: Float = 0f,
    val opacity: Float = 1f,
    val color: Int = 0xFFFFFF,
    val align: TextAlignment = TextAlignment.CENTER,
)

/**
 * Ajustes de mezcla del parche B sobre A (SPEC 4.3): brillo (-1..1, 0 = igual), contraste (1 = igual),
 * saturación (1 = igual) y [overall] = opacidad global del parche (1 = opaco).
 */
data class BlendConfig(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val overall: Float = 1f,
)

data class DisplayConfig(
    val timeBox: BoxRect = BoxRect(0.1f, 0.10f, 0.8f, 0.12f),
    val timeLook: TextLook = TextLook(size = 0.075f, weight = 300),
    val dateBox: BoxRect = BoxRect(0.05f, 0.13f, 0.5f, 0.04f),
    val dateLook: TextLook = TextLook(size = 0.017f, weight = 400, align = TextAlignment.START),
    val dateFormat: DateFormat = DateFormat.EN_SHORT,
    val blend: BlendConfig = BlendConfig(),
    /** Reloj apilado en dos líneas (horas encima de minutos), como el de la pantalla de bloqueo de Pixel. */
    val timeStacked: Boolean = false,
) {
    /** Cambia entre reloj de una línea y apilado, con una caja y un tamaño de partida razonables para cada modo. */
    fun withStacked(stacked: Boolean): DisplayConfig =
        if (stacked == timeStacked) this
        else if (stacked) copy(
            timeStacked = true,
            timeBox = BoxRect(0.15f, 0.30f, 0.7f, 0.40f),
            timeLook = timeLook.copy(size = STACKED_SIZE, weight = 200),
        ) else copy(
            timeStacked = false,
            timeBox = DEFAULT_TIME_BOX,
            timeLook = timeLook.copy(size = DEFAULT_TIME_SIZE, weight = 300),
        )

    /** Serialización compacta (una sola cadena) para guardarla en [com.rewind.ui.KeyValueStore]. */
    fun encode(): String {
        val v = mutableListOf<String>()
        fun box(b: BoxRect) { v += listOf(b.x, b.y, b.w, b.h).map { it.toString() } }
        fun look(l: TextLook) {
            v += listOf(l.size.toString(), l.weight.toString(), l.spacing.toString(), l.opacity.toString(),
                l.color.toString(), l.align.ordinal.toString())
        }
        box(timeBox); look(timeLook); box(dateBox); look(dateLook)
        v += dateFormat.ordinal.toString()
        v += listOf(blend.brightness, blend.contrast, blend.saturation, blend.overall).map { it.toString() }
        v += if (timeStacked) "1" else "0"
        return "v2;" + v.joinToString(";")
    }

    companion object {
        private const val FIELDS = 4 + 6 + 4 + 6 + 1 + 4
        const val STACKED_SIZE = 0.22f
        const val DEFAULT_TIME_SIZE = 0.075f
        val DEFAULT_TIME_BOX = BoxRect(0.1f, 0.10f, 0.8f, 0.12f)

        /** null si la cadena no es válida (versión distinta, campos que faltan o valores corruptos). */
        fun decode(s: String?): DisplayConfig? {
            if (s == null) return null
            val parts = s.split(";")
            // v1 (sin reloj apilado) sigue siendo legible; v2 añade el campo final.
            val ok = (parts[0] == "v1" && parts.size == FIELDS + 1) || (parts[0] == "v2" && parts.size == FIELDS + 2)
            if (!ok) return null
            val p = parts.drop(1)
            var i = 0
            fun f(): Float? = p[i++].toFloatOrNull()
            fun n(): Int? = p[i++].toIntOrNull()
            fun box(): BoxRect? {
                val x = f(); val y = f(); val w = f(); val h = f()
                return if (x == null || y == null || w == null || h == null) null else BoxRect(x, y, w, h).clamped()
            }
            fun look(): TextLook? {
                val size = f(); val weight = n(); val spacing = f(); val opacity = f(); val color = n(); val align = n()
                if (size == null || weight == null || spacing == null || opacity == null || color == null || align == null) return null
                val a = TextAlignment.entries.getOrNull(align) ?: return null
                return TextLook(size.coerceIn(0.005f, 0.3f), weight.coerceIn(100, 900), spacing.coerceIn(-0.3f, 1f),
                    opacity.coerceIn(0f, 1f), color and 0xFFFFFF, a)
            }
            val timeBox = box() ?: return null
            val timeLook = look() ?: return null
            val dateBox = box() ?: return null
            val dateLook = look() ?: return null
            val df = DateFormat.entries.getOrNull(n() ?: return null) ?: return null
            val b = f(); val c = f(); val sat = f(); val o = f()
            if (b == null || c == null || sat == null || o == null) return null
            val stacked = if (parts[0] == "v2") p[i] == "1" else false
            return DisplayConfig(
                timeBox, timeLook, dateBox, dateLook, df,
                BlendConfig(b.coerceIn(-1f, 1f), c.coerceIn(0f, 3f), sat.coerceIn(0f, 3f), o.coerceIn(0f, 1f)),
                stacked,
            )
        }
    }
}

/**
 * Matriz de color 4x5 (formato Android/Compose, desplazamientos en 0..255) que aplica saturación,
 * contraste y brillo en este orden de efecto. Saturación y contraste conmutan (la saturación deja el gris igual).
 */
fun blendColorMatrix(b: BlendConfig): FloatArray {
    val s = b.saturation
    val lr = 0.2126f; val lg = 0.7152f; val lb = 0.0722f
    val sr = (1 - s) * lr; val sg = (1 - s) * lg; val sb = (1 - s) * lb
    val c = b.contrast
    val t = 128f * (1 - c) + b.brightness * 255f
    // rows: R, G, B (con saturación), luego contraste y desplazamiento
    return floatArrayOf(
        c * (sr + s), c * sg, c * sb, 0f, t,
        c * sr, c * (sg + s), c * sb, 0f, t,
        c * sr, c * sg, c * (sb + s), 0f, t,
        0f, 0f, 0f, 1f, 0f,
    )
}
