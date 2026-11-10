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
        assertEquals("10:03", p.reset().shown("2026-01-01T10:00:30Z"))
        assertEquals(Phase.ARMED, p.reset().phaseAt(at("2026-01-01T10:00:30Z")))
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
        assertEquals(0f, p.reset().wakeFractionAt(at("2026-01-01T10:00:09Z")))
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

class GridInputTest {
    private fun at(s: String) = Instant.parse(s)

    @Test fun countsTapsAndConfirmsAfterPause() {
        var g = GridInput()
        g = g.tap(at("2026-01-01T10:00:00.000Z"))
        g = g.tap(at("2026-01-01T10:00:00.400Z"))
        g = g.tap(at("2026-01-01T10:00:00.800Z"))
        assertEquals(3, g.count)
        assertEquals(1_500L, g.remainingMs(at("2026-01-01T10:00:00.800Z")))
        assertEquals(700L, g.remainingMs(at("2026-01-01T10:00:01.600Z")))
        assertEquals(0L, g.remainingMs(at("2026-01-01T10:00:02.300Z")))
    }

    @Test fun emptyHasNothingToConfirm() {
        assertEquals(0L, GridInput().remainingMs(at("2026-01-01T10:00:00Z")))
    }

    @Test fun countIsCappedAtMax() {
        var g = GridInput(count = GridInput.MAX)
        g = g.tap(at("2026-01-01T10:00:00Z"))
        assertEquals(GridInput.MAX, g.count)
    }

    @Test fun withMinutesChangesOffsetOnlyWhileArmed() {
        val armed = Performance(PerformanceConfig(extraMinutes = 3)).withMinutes(7)
        assertEquals(7, armed.startOffset)
        assertEquals(PerformanceConfig(extraMinutes = 3).extraMinutes, armed.reset().withMinutes(3).config.extraMinutes)
        val fired = armed.trigger(at("2026-01-01T10:00:00Z"))
        assertEquals(7, fired.withMinutes(2).startOffset) // ya disparado: no cambia
        assertEquals(999, Performance(PerformanceConfig()).withMinutes(5000).config.extraMinutes)
        assertEquals(1, Performance(PerformanceConfig()).withMinutes(0).config.extraMinutes)
    }
}
