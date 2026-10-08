package com.kikepb.squadfy.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Optional date range of the statistics (spec 012 RN-B1): local dates of the club, both inclusive.
 * A missing bound leaves that side open.
 */
data class StatsPeriod(val from: LocalDate?, val to: LocalDate?) {

    init {
        require(from == null || to == null || !from.isAfter(to)) { "from cannot be after to" }
    }

    fun contains(instant: Instant, zone: ZoneId): Boolean {
        val date = instant.atZone(zone).toLocalDate()
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to))
    }

    companion object {
        val ALL = StatsPeriod(from = null, to = null)
    }
}
