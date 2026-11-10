package com.rewind.time

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class PerformanceTest {
    private val utc = TimeZone.UTC
    private fun at(s: String) = Instant.parse(s)
    private fun Performance.shown(now: String) = formatTime(displayAt(at(now)).local(utc), TimeFormat.H24_TWO_DIGITS)

    private val base = PerformanceConfig(extraMinutes = 3, delayMs = 4_000, speed = AnimationSpeed.REGULAR)

    @Test fun armedShowsRealPlusN() {
        val p = Performance(base)
        assertEquals(Phase.ARMED, p.phaseAt(at("2026-01-01T10:00:10Z")))
        assertEquals("10:03", p.shown("2026-01-01T10:00:10Z"))
        assertEquals("10:04", p.shown("2026-01-01T10:01:00Z")) // avanza con el minuto real
    }

    @Test fun delayThenAnimationThenSynced() {
        val p = Performance(base).trigger(at("2026-01-01T10:00:10Z"))
        assertEquals(Phase.DELAY, p.phaseAt(at("2026-01-01T10:00:13.9Z")))
        assertEquals("10:03", p.shown("2026-01-01T10:00:13.9Z"))
        assertEquals(Phase.ANIMATING, p.phaseAt(at("2026-01-01T10:00:14Z")))
        assertEquals("10:02", p.shown("2026-01-01T10:00:14.5Z"))
        assertEquals("10:01", p.shown("2026-01-01T10:00:15Z"))
        assertEquals("10:00", p.shown("2026-01-01T10:00:15.5Z"))
        assertEquals(Phase.SYNCED_HOLD, p.phaseAt(at("2026-01-01T10:00:15.5Z")))
        assertEquals("10:01", p.shown("2026-01-01T10:01:00Z")) // sigue la hora real
    }

    @Test fun realMinuteChangeDuringAnimationStillEndsOnRealTime() {
        val cfg = base.copy(extraMinutes = 5, delayMs = 1_000, speed = AnimationSpeed.SLOW)
        val p = Performance(cfg).trigger(at("2026-01-01T10:00:55Z"))
        assertEquals("10:02", p.shown("2026-01-01T10:00:59Z")) // 3 pasos hechos: 10:00 + 2 restantes
        assertEquals("10:02", p.shown("2026-01-01T10:01:00Z")) // minuto real 10:01 + 1 restante
        assertEquals("10:01", p.shown("2026-01-01T10:01:01Z")) // termina en la hora real vigente
    }

    @Test fun forwardsStartsBehindAndEndsOnReal() {
        val p = Performance(base.copy(direction = Direction.FORWARDS)).trigger(at("2026-01-01T10:00:00Z"))
        assertEquals("09:57", p.shown("2026-01-01T10:00:03Z"))
        assertEquals("09:58", p.shown("2026-01-01T10:00:04.5Z"))
        assertEquals("10:00", p.shown("2026-01-01T10:00:05.5Z"))
    }

    @Test fun noAnimationJumpsAfterDelay() {
        val p = Performance(base.copy(animate = false)).trigger(at("2026-01-01T10:00:00Z"))
        assertEquals("10:03", p.shown("2026-01-01T10:00:03.9Z"))
        assertEquals("10:00", p.shown("2026-01-01T10:00:04Z"))
    }

    @Test fun largeNCrossesHoursAndIsAccelerated() {
        val p = Performance(base.copy(extraMinutes = 150, delayMs = 1_000)).trigger(at("2026-01-01T23:30:00Z"))
        assertEquals("02:00", p.shown("2026-01-01T23:30:00Z"))
        assertEquals(20_000L / 150, p.stepMs)
        assertEquals(Phase.SYNCED_HOLD, p.phaseAt(at("2026-01-01T23:30:22Z")))
        assertEquals("23:30", p.shown("2026-01-01T23:30:22Z"))
    }

    @Test fun secondTriggerIgnoredAndResetRestores() {
        val p = Performance(base).trigger(at("2026-01-01T10:00:00Z"))
        assertEquals(p, p.trigger(at("2026-01-01T10:00:02Z")))
        assertEquals("10:03", p.reset(at("2026-01-01T10:00:30Z")).shown("2026-01-01T10:00:30Z"))
        assertEquals(Phase.ARMED, p.reset(at("2026-01-01T10:00:30Z")).phaseAt(at("2026-01-01T10:00:30Z")))
    }
}

class WakeAndAlertTest {
    private fun at(s: String) = Instant.parse(s)

    @Test fun withoutAlwaysOnScreenIsAlwaysOn() {
        assertEquals(1f, Performance(PerformanceConfig()).wakeFractionAt(at("2026-01-01T10:00:00Z")))
    }

    @Test fun alwaysOnIsOffUntilTrigger() {
        val p = Performance(PerformanceConfig(alwaysOn = true, autoWake = true))
        assertEquals(0f, p.wakeFractionAt(at("2026-01-01T10:00:10Z")))
    }

    @Test fun autoWakeLightsAtTriggerWithFade() {
        val p = Performance(PerformanceConfig(alwaysOn = true, autoWake = true)).trigger(at("2026-01-01T10:00:00Z"))
        assertEquals(0f, p.wakeFractionAt(at("2026-01-01T10:00:00Z")))
        assertEquals(0.5f, p.wakeFractionAt(at("2026-01-01T10:00:00.200Z")), 1e-4f)
        assertEquals(1f, p.wakeFractionAt(at("2026-01-01T10:00:00.500Z")))
    }

    @Test fun withoutAutoWakeLightsWhenAnimationStarts() {
        val p = Performance(PerformanceConfig(alwaysOn = true, autoWake = false, delayMs = 4_000))
            .trigger(at("2026-01-01T10:00:00Z"))
        assertEquals(0f, p.wakeFractionAt(at("2026-01-01T10:00:03.900Z")))
        assertEquals(1f, p.wakeFractionAt(at("2026-01-01T10:00:04.500Z")))
    }

    @Test fun resetTurnsScreenOffAgain() {
        val p = Performance(PerformanceConfig(alwaysOn = true, autoWake = true)).trigger(at("2026-01-01T10:00:00Z"))
        assertEquals(0f, p.reset(at("2026-01-01T10:00:09Z")).wakeFractionAt(at("2026-01-01T10:00:09Z")))
    }

    @Test fun minuteAlertWindow() {
        assertEquals(false, isMinuteAlertActive(at("2026-01-01T10:00:49.9Z"), 10))
        assertEquals(true, isMinuteAlertActive(at("2026-01-01T10:00:50Z"), 10))
        assertEquals(true, isMinuteAlertActive(at("2026-01-01T10:00:59Z"), 10))
        assertEquals(false, isMinuteAlertActive(at("2026-01-01T10:01:00Z"), 10)) // minuto nuevo: faltan 60 s
        assertEquals(5_000L, millisToMinuteAlert(at("2026-01-01T10:00:45Z"), 10))
        assertEquals(0L, millisToMinuteAlert(at("2026-01-01T10:00:55Z"), 10))
    }
}

class NumberEntryTest {
    private fun at(s: String) = Instant.parse(s)
    private val t0 = at("2026-01-01T10:00:00Z")

    @Test fun twoDigitsCompleteAndGiveValue() {
        var e = NumberEntry(lastInputAt = t0)
        e = e.press(1, t0)
        assertEquals(false, e.isComplete)
        e = e.press(7, t0)
        assertEquals(true, e.isComplete)
        assertEquals(17, e.value)
    }

    @Test fun leadingZeroGivesSingleDigitValue() {
        val e = NumberEntry(lastInputAt = t0).press(0, t0).press(7, t0)
        assertEquals(7, e.value)
        assertEquals(0, NumberEntry(lastInputAt = t0).press(0, t0).press(0, t0).value)
    }

    @Test fun closesByItselfAfterInactivity() {
        val e = NumberEntry(lastInputAt = t0).press(4, at("2026-01-01T10:00:03Z"))
        assertEquals(10_000L, e.remainingMs(at("2026-01-01T10:00:03Z")))
        assertEquals(4_000L, e.remainingMs(at("2026-01-01T10:00:09Z")))
        assertEquals(0L, e.remainingMs(at("2026-01-01T10:00:14Z")))
    }

    @Test fun keypadOccupiesOnlyTheBottomHalf() {
        // mitad superior: sin teclas
        assertEquals(null, keypadDigitAt(0.5f, 0.1f))
        assertEquals(null, keypadDigitAt(0.1f, 0.49f))
        // mitad inferior: 4 filas (cada una 12,5 % de la pantalla) × 3 columnas
        assertEquals(1, keypadDigitAt(0.1f, 0.51f))
        assertEquals(2, keypadDigitAt(0.5f, 0.55f))
        assertEquals(3, keypadDigitAt(0.9f, 0.60f))
        assertEquals(4, keypadDigitAt(0.1f, 0.64f))
        assertEquals(5, keypadDigitAt(0.5f, 0.69f))
        assertEquals(7, keypadDigitAt(0.1f, 0.76f))
        assertEquals(9, keypadDigitAt(0.9f, 0.80f))
        assertEquals(0, keypadDigitAt(0.1f, 0.90f))   // fila de abajo entera = 0
        assertEquals(0, keypadDigitAt(0.5f, 0.99f))
        assertEquals(0, keypadDigitAt(1.0f, 1.0f))     // bordes: no se salen del teclado
        assertEquals(1, keypadDigitAt(0f, 0.5f))
    }

    @Test fun withMinutesChangesOffsetOnlyWhileArmed() {
        val armed = Performance(PerformanceConfig(extraMinutes = 3)).withMinutes(7)
        assertEquals(7, armed.startOffset)
        val fired = armed.trigger(at("2026-01-01T10:00:00Z"))
        assertEquals(7, fired.withMinutes(2).startOffset) // ya disparado: no cambia
        assertEquals(99, Performance(PerformanceConfig()).withMinutes(99).config.extraMinutes)
        assertEquals(999, Performance(PerformanceConfig()).withMinutes(5000).config.extraMinutes)
        assertEquals(1, Performance(PerformanceConfig()).withMinutes(0).config.extraMinutes)
    }
}

class DimmedAodTest {
    private fun at(s: String) = Instant.parse(s)
    private val t0 = at("2026-01-01T10:00:00Z")
    private val cfg = PerformanceConfig(alwaysOn = true, aodStyle = AodStyle.DIMMED, awakeSeconds = 5)

    @Test fun dimmedByDefaultAndTouchWakesItWithFade() {
        val p = Performance(cfg)
        assertEquals(0f, p.focusAt(t0))
        val touched = p.touch(t0)
        assertEquals(0.5f, touched.focusAt(at("2026-01-01T10:00:00.200Z")), 1e-4f)
        assertEquals(1f, touched.focusAt(at("2026-01-01T10:00:01Z")))
    }

    @Test fun staysAwakeForConfiguredSecondsThenDimsAgain() {
        val p = Performance(cfg).touch(t0)
        assertEquals(1f, p.focusAt(at("2026-01-01T10:00:05.300Z")))     // 0,4 s de subida + 5 s despierta = 5,4 s
        assertEquals(0.5f, p.focusAt(at("2026-01-01T10:00:05.700Z")), 1e-4f) // a mitad de la bajada (0,6 s)
        assertEquals(0f, p.focusAt(at("2026-01-01T10:00:06.100Z")))
    }

    @Test fun anotherTouchExtendsTheAwakeTime() {
        val p = Performance(cfg).touch(t0).touch(at("2026-01-01T10:00:04Z"))
        assertEquals(1f, p.focusAt(at("2026-01-01T10:00:09Z")))   // sin el segundo toque ya estaría bajando
        assertEquals(0f, p.focusAt(at("2026-01-01T10:00:10.500Z")))
    }

    @Test fun touchDuringFadeOutContinuesFromCurrentLevel() {
        val p = Performance(cfg).touch(t0)
        val midFade = at("2026-01-01T10:00:05.700Z") // foco 0,5 bajando
        val again = p.touch(midFade)
        assertEquals(0.5f, again.focusAt(midFade), 1e-3f) // sin salto
        assertEquals(1f, again.focusAt(at("2026-01-01T10:00:06.200Z")))
    }

    @Test fun touchDoesNothingWithBlackStyleOrWithoutAlwaysOn() {
        val black = Performance(cfg.copy(aodStyle = AodStyle.BLACK))
        assertEquals(black, black.touch(t0))
        val off = Performance(PerformanceConfig())
        assertEquals(off, off.touch(t0))
        assertEquals(1f, off.focusAt(t0))
    }

    @Test fun triggerWithAutoWakeBrightensEvenWithoutTouch() {
        val p = Performance(cfg.copy(autoWake = true)).trigger(t0)
        assertEquals(1f, p.focusAt(at("2026-01-01T10:00:01Z")))
    }

    @Test fun resetKeepsTheScreenAwakeAndDimsOnlyAfterInactivity() {
        val p = Performance(cfg).touch(t0)
        val r = p.reset(at("2026-01-01T10:00:02Z"))             // reiniciar cuenta como actividad
        assertEquals(null, r.triggeredAt)
        assertEquals(1f, r.focusAt(at("2026-01-01T10:00:02Z")))
        assertEquals(1f, r.focusAt(at("2026-01-01T10:00:06Z")))   // sigue despierta
        assertEquals(1f, r.focusAt(at("2026-01-01T10:00:07Z")))   // 5 s de calma justo cumplidos
        assertEquals(0.5f, r.focusAt(at("2026-01-01T10:00:07.300Z")), 1e-3f)
        assertEquals(0f, r.focusAt(at("2026-01-01T10:00:08Z")))
    }

    @Test fun resetFromDimmedStateFadesInInsteadOfJumping() {
        val r = Performance(cfg).reset(at("2026-01-01T10:00:00Z"))
        assertEquals(0f, r.focusAt(at("2026-01-01T10:00:00Z")))
        assertEquals(0.5f, r.focusAt(at("2026-01-01T10:00:00.200Z")), 1e-3f)
        assertEquals(1f, r.focusAt(at("2026-01-01T10:00:00.500Z")))
    }

    @Test fun resetWithNewConfigDiscardsLiveMinutes() {
        val live = Performance(cfg).withMinutes(42)
        assertEquals(42, live.config.extraMinutes)
        assertEquals(cfg.extraMinutes, live.reset(t0, cfg).config.extraMinutes)
    }

    @Test fun resetWithBlackStyleTurnsScreenOffAgain() {
        val black = Performance(PerformanceConfig(alwaysOn = true, aodStyle = AodStyle.BLACK)).trigger(t0)
        assertEquals(0f, black.reset(at("2026-01-01T10:00:09Z")).focusAt(at("2026-01-01T10:00:10Z")))
    }

    @Test fun staysAwakeWhileTheEffectRunsAndDimsAgainAfterItEnds() {
        // despierta por un toque, dispara 1 s después. Efecto: retardo 4 s + animación 1,5 s (3 pasos de 0,5 s) = 5,5 s → termina en +6,5 s
        val p = Performance(cfg).touch(t0).trigger(at("2026-01-01T10:00:01Z"))
        assertEquals(1f, p.focusAt(at("2026-01-01T10:00:03Z")))    // retardo
        assertEquals(1f, p.focusAt(at("2026-01-01T10:00:05.600Z"))) // el toque ya "caducaría", pero el efecto sigue: no se atenúa
        assertEquals(1f, p.focusAt(at("2026-01-01T10:00:06.400Z"))) // animación
        assertEquals(Phase.SYNCED_HOLD, p.phaseAt(at("2026-01-01T10:00:06.600Z")))
        // terminó en +6,5 s: despierta 5 s más (+0,4 s de partida) y vuelve a atenuarse
        assertEquals(1f, p.focusAt(at("2026-01-01T10:00:11.400Z")))
        assertEquals(0.5f, p.focusAt(at("2026-01-01T10:00:11.800Z")), 1e-3f)
        assertEquals(0f, p.focusAt(at("2026-01-01T10:00:12.300Z")))
    }

    @Test fun touchAfterTheEffectWakesItAgainWithoutTriggering() {
        val p = Performance(cfg).touch(t0).trigger(at("2026-01-01T10:00:01Z"))
        val late = at("2026-01-01T10:00:20Z")
        assertEquals(0f, p.focusAt(late))                              // ya atenuada
        val touched = p.touch(late)
        assertEquals(p.triggeredAt, touched.triggeredAt)
        assertEquals(0.5f, touched.focusAt(at("2026-01-01T10:00:20.200Z")), 1e-3f)
        assertEquals(1f, touched.focusAt(at("2026-01-01T10:00:21Z")))
    }

    @Test fun touchDuringTheEffectProlongsTheWakeAfterIt() {
        val p = Performance(cfg).touch(t0).trigger(at("2026-01-01T10:00:01Z"))
        val touched = p.touch(at("2026-01-01T10:00:09Z"))            // ya post-efecto, aún despierta
        assertEquals(1f, touched.focusAt(at("2026-01-01T10:00:14Z")))  // sin el toque ya estaría atenuada
    }

    @Test fun notAwakeAtTriggerWithoutAutoWakeLightsWhenAnimationStarts() {
        val p = Performance(cfg.copy(autoWake = false)).trigger(t0)      // atenuada, sin tocar
        assertEquals(0f, p.focusAt(at("2026-01-01T10:00:03.900Z")))
        assertEquals(1f, p.focusAt(at("2026-01-01T10:00:04.500Z")))
        // y tras terminar (4 s + 1,5 s) se vuelve a atenuar pasado el tiempo
        assertEquals(0f, p.focusAt(at("2026-01-01T10:00:13Z")))
    }

    @Test fun msUntilDimStarts() {
        val p = Performance(cfg).touch(t0)
        assertEquals(5_400L, p.msUntilDimStarts(t0))
        assertEquals(1_400L, p.msUntilDimStarts(at("2026-01-01T10:00:04Z")))
        assertEquals(null, p.msUntilDimStarts(at("2026-01-01T10:00:06Z")))
        assertEquals(null, Performance(cfg).msUntilDimStarts(t0))
        // tras el efecto también hay que despertar para atenuar
        val fired = Performance(cfg).touch(t0).trigger(at("2026-01-01T10:00:01Z"))
        assertEquals(null, fired.msUntilDimStarts(at("2026-01-01T10:00:03Z")))     // mientras corre el efecto
        assertEquals(5_000L, fired.msUntilDimStarts(at("2026-01-01T10:00:06.500Z"))) // el efecto termina en +6,5 s; el ciclo parte 0,4 s antes
    }
}
