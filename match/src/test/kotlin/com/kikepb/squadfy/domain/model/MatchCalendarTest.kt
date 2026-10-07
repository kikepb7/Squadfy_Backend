package com.kikepb.squadfy.domain.model

import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals

class MatchCalendarTest {

    private val madrid = ZoneId.of("Europe/Madrid")

    private fun madridInstant(value: String): Instant = LocalDateTime.parse(value).atZone(madrid).toInstant()

    @Test
    fun `next match date is strictly after today`() {
        val wednesday = LocalDate.of(2026, 10, 7)

        assertEquals(LocalDate.of(2026, 10, 8), MatchCalendar.nextMatchDate(wednesday, DayOfWeek.THURSDAY))
        assertEquals(LocalDate.of(2026, 10, 14), MatchCalendar.nextMatchDate(wednesday, DayOfWeek.WEDNESDAY))
        assertEquals(LocalDate.of(2026, 10, 12), MatchCalendar.nextMatchDate(wednesday, DayOfWeek.MONDAY))
    }

    @Test
    fun `announcement opens the day after the previous match and closes at 22h the day before`() {
        val window = MatchCalendar.announcementWindow(
            matchAt = madridInstant("2026-10-14T20:00"),
            zone = madrid,
            previousMatchAt = madridInstant("2026-10-07T20:00"),
            now = madridInstant("2026-10-07T22:00")
        )

        assertEquals(madridInstant("2026-10-08T00:00"), window.opensAt)
        assertEquals(madridInstant("2026-10-13T22:00"), window.closesAt)
    }

    @Test
    fun `first announcement of a club opens immediately`() {
        val now = madridInstant("2026-10-05T10:00")

        val window = MatchCalendar.announcementWindow(
            matchAt = madridInstant("2026-10-08T20:00"),
            zone = madrid,
            previousMatchAt = null,
            now = now
        )

        assertEquals(now, window.opensAt)
        assertEquals(madridInstant("2026-10-07T22:00"), window.closesAt)
    }

    @Test
    fun `a match created after the cut-off accepts enrollments until kickoff`() {
        val now = madridInstant("2026-10-07T22:30")
        val kickoff = madridInstant("2026-10-08T20:00")

        val window = MatchCalendar.announcementWindow(matchAt = kickoff, zone = madrid, previousMatchAt = null, now = now)

        assertEquals(now, window.opensAt)
        assertEquals(kickoff, window.closesAt)
    }

    @Test
    fun `window is computed in the club time zone across daylight saving changes`() {
        // DST ends in Madrid on 2026-10-25.
        val window = MatchCalendar.announcementWindow(
            matchAt = madridInstant("2026-10-28T20:00"),
            zone = madrid,
            previousMatchAt = madridInstant("2026-10-21T20:00"),
            now = madridInstant("2026-10-21T22:00")
        )

        assertEquals(Instant.parse("2026-10-21T22:00:00Z"), window.opensAt)
        assertEquals(Instant.parse("2026-10-27T21:00:00Z"), window.closesAt)
    }

    @Test
    fun `close and draw follow the club configuration and the draw is never before the close`() {
        val zone = ZoneId.of("Europe/Madrid")
        val matchAt = LocalDateTime.parse("2026-10-08T20:00").atZone(zone).toInstant()
        val now = LocalDateTime.parse("2026-10-05T10:00").atZone(zone).toInstant()

        val window = MatchCalendar.announcementWindow(
            matchAt = matchAt,
            zone = zone,
            previousMatchAt = null,
            now = now,
            close = DeadlineRule(daysBefore = 1, time = LocalTime.of(21, 0)),
            draw = DeadlineRule(daysBefore = 0, time = LocalTime.of(12, 0))
        )
        assertEquals(LocalDateTime.parse("2026-10-07T21:00").atZone(zone).toInstant(), window.closesAt)
        assertEquals(LocalDateTime.parse("2026-10-08T12:00").atZone(zone).toInstant(), window.drawAt)

        val lateMatch = MatchCalendar.announcementWindow(
            matchAt = matchAt,
            zone = zone,
            previousMatchAt = null,
            now = LocalDateTime.parse("2026-10-07T23:00").atZone(zone).toInstant()
        )
        assertEquals(matchAt, lateMatch.closesAt)
        assertEquals(matchAt, lateMatch.drawAt)
    }
}
