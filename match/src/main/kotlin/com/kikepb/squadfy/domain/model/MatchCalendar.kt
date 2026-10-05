package com.kikepb.squadfy.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Pure date rules for the weekly match cycle (see specs/002-match-cycle/spec.md):
 * - The announcement opens the day after the previous match was played.
 * - It closes at 22:00 (club time) the day before the match. A match created after that
 *   cut-off accepts enrollments until kickoff.
 * All calendar arithmetic happens in the club's time zone.
 */
object MatchCalendar {

    val ENROLLMENT_CUTOFF: LocalTime = LocalTime.of(22, 0)

    /** First [matchDay] strictly after [today]. */
    fun nextMatchDate(today: LocalDate, matchDay: DayOfWeek): LocalDate {
        val daysUntil = (matchDay.value - today.dayOfWeek.value + 7) % 7
        return today.plusDays(if (daysUntil == 0) 7L else daysUntil.toLong())
    }

    fun announcementWindow(
        matchAt: Instant,
        zone: ZoneId,
        previousMatchAt: Instant?,
        now: Instant
    ): AnnouncementWindow {
        val matchDate = matchAt.atZone(zone).toLocalDate()

        val closesAt = matchDate.minusDays(1).atTime(ENROLLMENT_CUTOFF).atZone(zone).toInstant()
            .takeIf { it.isAfter(now) }
            ?: matchAt

        val opensAt = previousMatchAt
            ?.atZone(zone)?.toLocalDate()?.plusDays(1)?.atStartOfDay(zone)?.toInstant()
            ?.takeIf { it.isBefore(closesAt) }
            ?: now

        return AnnouncementWindow(opensAt = opensAt, closesAt = closesAt)
    }

    data class AnnouncementWindow(
        val opensAt: Instant,
        val closesAt: Instant
    )
}
