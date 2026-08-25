package com.kikepb.squadfy.service

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class MatchWeekCalculatorTest {

    @Test
    fun `sunday match keeps monday-to-saturday signup window in the same week`() {
        // Monday 2026-08-24
        val today = LocalDate.of(2026, 8, 24)
        val week = MatchWeekCalculator.nextMatchWeek(today = today, dayOfWeek = DayOfWeek.SUNDAY)

        assertEquals(LocalDate.of(2026, 8, 30), week.matchDate)
        assertEquals(LocalDate.of(2026, 8, 24), week.signupOpensDate)
        assertEquals(LocalDate.of(2026, 8, 29), week.signupClosesDate)
    }

    @Test
    fun `match day already passed this week rolls over to next week`() {
        // Match day is Monday, but today is already Wednesday of that week.
        val today = LocalDate.of(2026, 8, 26)
        val week = MatchWeekCalculator.nextMatchWeek(today = today, dayOfWeek = DayOfWeek.MONDAY)

        assertEquals(LocalDate.of(2026, 8, 31), week.matchDate)
        assertEquals(LocalDate.of(2026, 8, 31), week.signupOpensDate)
        assertEquals(LocalDate.of(2026, 9, 5), week.signupClosesDate)
    }

    @Test
    fun `match day is today`() {
        val today = LocalDate.of(2026, 8, 26)
        val week = MatchWeekCalculator.nextMatchWeek(today = today, dayOfWeek = DayOfWeek.WEDNESDAY)

        assertEquals(today, week.matchDate)
        assertEquals(LocalDate.of(2026, 8, 24), week.signupOpensDate)
        assertEquals(LocalDate.of(2026, 8, 29), week.signupClosesDate)
    }
}
