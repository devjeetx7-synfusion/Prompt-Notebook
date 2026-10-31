package com.synfusion.snipnote.util

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class TimeFormatTest {

    private val zone = ZoneId.of("UTC")

    @Before
    fun setUp() {
        Locale.setDefault(Locale.UK)
    }

    private fun millisAt(date: LocalDate, hour: Int = 12): Long =
        ZonedDateTime.of(date.atTime(hour, 0), zone).toInstant().toEpochMilli()

    @Test
    fun `greeting depends on the hour`() {
        assertEquals("Good morning", TimeFormat.greeting(8))
        assertEquals("Good afternoon", TimeFormat.greeting(14))
        assertEquals("Good evening", TimeFormat.greeting(19))
        assertEquals("Working late", TimeFormat.greeting(2))
    }

    @Test
    fun `relative time handles minutes hours and days`() {
        val now = millisAt(LocalDate.of(2026, 3, 10), 12)
        assertEquals("Just now", TimeFormat.relative(now - 30_000, now, zone))
        assertEquals("5m ago", TimeFormat.relative(now - 5 * 60_000, now, zone))
        assertEquals("3h ago", TimeFormat.relative(now - 3 * 3_600_000, now, zone))
        assertEquals("Yesterday", TimeFormat.relative(now - 24 * 3_600_000, now, zone))
        assertEquals("3d ago", TimeFormat.relative(now - 3 * 24 * 3_600_000, now, zone))
    }

    @Test
    fun `old dates fall back to a formatted date`() {
        val now = millisAt(LocalDate.of(2026, 3, 10), 12)
        val old = millisAt(LocalDate.of(2025, 1, 2), 12)
        assertEquals("2 Jan 2025", TimeFormat.relative(old, now, zone))
        assertEquals("2 Jan 2025", TimeFormat.dayMonthYear(old, zone))
    }

    @Test
    fun `future timestamps do not produce negative values`() {
        val now = millisAt(LocalDate.of(2026, 3, 10), 12)
        assertEquals("Just now", TimeFormat.relative(now + 60_000, now, zone))
    }
}
