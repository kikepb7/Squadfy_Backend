package com.kikepb.squadfy.service

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class MatchWeek(val matchDate: LocalDate, val signupOpensDate: LocalDate, val signupClosesDate: LocalDate)

/**
 * Resolves the sign-up window for a club's weekly match: sign-ups run Monday through Saturday of
 * the match's own week, whatever day the match itself falls on (typically, but not necessarily, a
 * Sunday).
 */
object MatchWeekCalculator {

    fun nextMatchWeek(today: LocalDate, dayOfWeek: DayOfWeek): MatchWeek {
        val matchDate = today.with(TemporalAdjusters.nextOrSame(dayOfWeek))
        val monday = matchDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val saturday = monday.plusDays(5)
        return MatchWeek(matchDate = matchDate, signupOpensDate = monday, signupClosesDate = saturday)
    }
}
