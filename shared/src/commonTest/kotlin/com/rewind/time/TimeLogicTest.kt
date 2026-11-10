package com.rewind.time

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeLogicTest {
    private val utc = TimeZone.UTC
    private fun at(s: String) = Instant.parse(s)
    private fun show(t: Instant, f: TimeFormat = TimeFormat.H24_TWO_DIGITS) = formatTime(t.local(utc), f)

    @Test fun backwardsAddsMinutes() {
        val d = displayTime(at("2026-01-01T19:56:42Z"), offsetMinutes(Direction.BACKWARDS, 5))
        assertEquals("20:01", show(d))
    }

    @Test fun forwardsSubtractsMinutes() {
        val d = displayTime(at("2026-01-01T19:56:42Z"), offsetMinutes(Direction.FORWARDS, 5))
        assertEquals("19:51", show(d))
    }

    @Test fun rolloverOfHourAndDayWithLargeN() {
        val d = displayTime(at("2026-01-01T23:30:00Z"), 150)
        assertEquals("02:00", show(d))
        assertEquals(2, d.local(utc).dayOfMonth)
    }

    @Test fun twelveHourShowsTwelve() {
        assertEquals("12:05", show(at("2026-01-01T00:05:00Z"), TimeFormat.H12))
        assertEquals("12:05", show(at("2026-01-01T12:05:00Z"), TimeFormat.H12))
        assertEquals("01:05", show(at("2026-01-01T13:05:00Z"), TimeFormat.H12_TWO_DIGITS))
    }

    @Test fun minuteChangesAtRealSecondZero() {
        assertEquals(1_000L, millisToNextMinute(at("2026-01-01T10:00:59Z")))
        assertEquals(60_000L, millisToNextMinute(at("2026-01-01T10:01:00Z")))
        assertEquals(at("2026-01-01T10:00:00Z"), truncMinute(at("2026-01-01T10:00:59.999Z")))
    }
}
