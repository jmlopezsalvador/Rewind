package com.rewind.display

import com.rewind.time.DateFormat
import com.rewind.time.formatDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DisplayConfigTest {
    @Test fun encodeDecodeRoundTrip() {
        val cfg = DisplayConfig(
            timeBox = BoxRect(0.2f, 0.3f, 0.5f, 0.1f),
            timeLook = TextLook(0.08f, 200, 0.05f, 0.9f, 0x112233, TextAlignment.END),
            dateFormat = DateFormat.ES_LONG,
            blend = BlendConfig(0.1f, 1.2f, 0.8f, 0.7f),
        )
        assertEquals(cfg, DisplayConfig.decode(cfg.encode()))
    }

    @Test fun decodeRejectsGarbage() {
        assertNull(DisplayConfig.decode(null))
        assertNull(DisplayConfig.decode(""))
        assertNull(DisplayConfig.decode("v2;1;2"))
        assertNull(DisplayConfig.decode(DisplayConfig().encode().replace("0.075", "x")))
        assertNotNull(DisplayConfig.decode(DisplayConfig().encode()))
    }

    @Test fun boxClampedStaysInsideScreen() {
        val b = BoxRect(0.9f, -0.2f, 0.5f, 0.001f).clamped()
        assertTrue(b.x + b.w <= 1f + 1e-6f && b.x >= 0f)
        assertTrue(b.y >= 0f)
        assertEquals(BoxRect.MIN, b.h)
    }

    @Test fun neutralBlendIsIdentityMatrix() {
        val m = blendColorMatrix(BlendConfig())
        val identity = floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
        m.indices.forEach { assertEquals(identity[it], m[it], 1e-4f, "índice $it") }
    }

    @Test fun zeroSaturationGivesGrey() {
        val m = blendColorMatrix(BlendConfig(saturation = 0f))
        // las tres filas de color son iguales
        for (j in 0 until 5) { assertEquals(m[j], m[5 + j], 1e-5f); assertEquals(m[j], m[10 + j], 1e-5f) }
    }

    @Test fun dateFormats() {
        val t = LocalDateTime(2026, 11, 10, 12, 0)
        assertEquals("Tue, Nov 10", formatDate(t, DateFormat.EN_SHORT))
        assertEquals("Tuesday 10 November", formatDate(t, DateFormat.EN_LONG))
        assertEquals("mar, 10 nov", formatDate(t, DateFormat.ES_SHORT))
        assertEquals("martes 10 de noviembre", formatDate(t, DateFormat.ES_LONG))
    }
}
