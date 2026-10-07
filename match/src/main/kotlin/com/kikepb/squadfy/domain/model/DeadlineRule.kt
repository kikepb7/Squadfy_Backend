package com.kikepb.squadfy.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** "N days before the match at a local time" (spec 008 RN-D1). */
data class DeadlineRule(
    val daysBefore: Int,
    val time: LocalTime
) {
    fun instantFor(matchDate: LocalDate, zone: ZoneId): Instant =
        matchDate.minusDays(daysBefore.toLong()).atTime(time).atZone(zone).toInstant()

    /** Minutes between this deadline and a kickoff at [matchTime] (positive = before kickoff). */
    fun minutesBefore(matchTime: LocalTime): Long =
        daysBefore * MINUTES_PER_DAY + (matchTime.toSecondOfDay() - time.toSecondOfDay()) / 60

    companion object {
        private const val MINUTES_PER_DAY = 24L * 60
        val DEFAULT = DeadlineRule(daysBefore = 1, time = LocalTime.of(22, 0))

        /** Close strictly before kickoff; draw at or after close and before kickoff (RN-D2). */
        fun validate(matchTime: LocalTime, close: DeadlineRule, draw: DeadlineRule): String? {
            val closeMinutes = close.minutesBefore(matchTime)
            val drawMinutes = draw.minutesBefore(matchTime)
            return when {
                close.daysBefore < 0 || draw.daysBefore < 0 -> "Days before the match cannot be negative"
                closeMinutes <= 0 -> "The announcement must close before the match starts"
                drawMinutes <= 0 -> "The teams must be drawn before the match starts"
                drawMinutes > closeMinutes -> "The teams cannot be drawn before the announcement closes"
                else -> null
            }
        }
    }
}
