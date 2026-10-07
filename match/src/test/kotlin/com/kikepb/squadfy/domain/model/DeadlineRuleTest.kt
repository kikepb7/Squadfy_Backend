package com.kikepb.squadfy.domain.model

import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DeadlineRuleTest {

    private val madrid = ZoneId.of("Europe/Madrid")
    private val kickoff = LocalTime.of(20, 0)

    @Test
    fun `deadline is the local time some days before the match`() {
        val rule = DeadlineRule(daysBefore = 1, time = LocalTime.of(21, 0))

        assertEquals(
            LocalDateTime.parse("2026-10-07T21:00").atZone(madrid).toInstant(),
            rule.instantFor(matchDate = LocalDate.parse("2026-10-08"), zone = madrid)
        )
    }

    @Test
    fun `default and same-day configurations are valid`() {
        assertNull(DeadlineRule.validate(kickoff, DeadlineRule.DEFAULT, DeadlineRule.DEFAULT))
        assertNull(DeadlineRule.validate(kickoff, DeadlineRule(1, LocalTime.of(21, 0)), DeadlineRule(0, LocalTime.of(12, 0))))
    }

    @Test
    fun `close after kickoff, draw before close or after kickoff are rejected`() {
        assertNotNull(DeadlineRule.validate(kickoff, DeadlineRule(0, LocalTime.of(20, 0)), DeadlineRule(0, LocalTime.of(20, 0))))
        assertNotNull(DeadlineRule.validate(kickoff, DeadlineRule(1, LocalTime.of(22, 0)), DeadlineRule(2, LocalTime.of(12, 0))))
        assertNotNull(DeadlineRule.validate(kickoff, DeadlineRule(1, LocalTime.of(22, 0)), DeadlineRule(0, LocalTime.of(21, 0))))
        assertNotNull(DeadlineRule.validate(kickoff, DeadlineRule(-1, LocalTime.of(22, 0)), DeadlineRule.DEFAULT))
    }
}
