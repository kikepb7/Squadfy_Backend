package com.kikepb.squadfy.domain.model

import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StatsPeriodTest {

    private val madrid = ZoneId.of("Europe/Madrid")

    @Test
    fun `bounds are inclusive local dates of the club`() {
        val september = StatsPeriod(from = LocalDate.parse("2026-09-01"), to = LocalDate.parse("2026-09-30"))

        // 30 Sep 23:30 in Madrid is still September although it is 21:30 UTC
        assertTrue(september.contains(Instant.parse("2026-09-30T21:30:00Z"), madrid))
        // 1 Oct 00:30 in Madrid is already October although it is 30 Sep in UTC
        assertFalse(september.contains(Instant.parse("2026-09-30T22:30:00Z"), madrid))
        assertTrue(september.contains(Instant.parse("2026-08-31T22:00:00Z"), madrid))
    }

    @Test
    fun `missing bounds are open and an inverted range is rejected`() {
        assertTrue(StatsPeriod.ALL.contains(Instant.parse("2000-01-01T00:00:00Z"), madrid))
        assertTrue(StatsPeriod(from = LocalDate.parse("2026-01-01"), to = null).contains(Instant.parse("2030-01-01T00:00:00Z"), madrid))
        assertFailsWith<IllegalArgumentException> { StatsPeriod(from = LocalDate.parse("2026-02-01"), to = LocalDate.parse("2026-01-01")) }
    }
}
