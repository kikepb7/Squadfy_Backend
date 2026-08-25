package com.kikepb.squadfy.infrastructure.scheduling

import com.kikepb.squadfy.infrastructure.database.entities.ClubMatchEntity.MatchStatusEntity
import com.kikepb.squadfy.infrastructure.database.repositories.ClubMatchRepository
import com.kikepb.squadfy.infrastructure.database.repositories.ClubRepository
import com.kikepb.squadfy.infrastructure.database.repositories.ClubScheduleExceptionRepository
import com.kikepb.squadfy.service.ClubService
import com.kikepb.squadfy.service.ClubService.TeamGenerationMode.AUTO
import com.kikepb.squadfy.service.MatchWeekCalculator
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Turns each club's configured weekly schedule into concrete [com.kikepb.squadfy.infrastructure.database.entities.ClubMatchEntity]
 * rows and, once a match's sign-up window has closed, into a balanced team draw - so members
 * don't have to be manually prompted every week (the "Monday to Saturday sign-up, Saturday
 * afternoon draw" flow described by the product).
 *
 * All times are handled in UTC; a per-club timezone is intentionally out of scope for this pass.
 */
@Component
class ClubMatchScheduler(
    private val clubRepository: ClubRepository,
    private val clubScheduleExceptionRepository: ClubScheduleExceptionRepository,
    private val clubMatchRepository: ClubMatchRepository,
    private val clubService: ClubService
) {
    private val logger = LoggerFactory.getLogger(ClubMatchScheduler::class.java)

    @Scheduled(cron = "0 10 0 * * *")
    fun createUpcomingWeeklyMatches() {
        val today = LocalDate.now(ZoneOffset.UTC)

        clubRepository.findAllByMatchDayOfWeekIsNotNull().forEach { club ->
            runCatching {
                val clubId = requireNotNull(club.id)
                val dayOfWeek = club.matchDayOfWeek ?: return@runCatching
                val week = MatchWeekCalculator.nextMatchWeek(today = today, dayOfWeek = dayOfWeek)

                if (clubScheduleExceptionRepository.existsByClubIdAndDate(clubId = clubId, date = week.matchDate)) return@runCatching

                val dayStart = week.matchDate.atStartOfDay(ZoneOffset.UTC).toInstant()
                val dayEnd = week.matchDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
                if (clubMatchRepository.existsByClubIdAndScheduledAtBetween(clubId = clubId, start = dayStart, end = dayEnd)) return@runCatching

                val scheduledAt = week.matchDate.atTime(club.matchStartTime ?: LocalTime.NOON).toInstant(ZoneOffset.UTC)
                val signupOpensAt = week.signupOpensDate.atStartOfDay(ZoneOffset.UTC).toInstant()
                val signupClosesAt = week.signupClosesDate.atTime(club.drawTime).toInstant(ZoneOffset.UTC)

                clubService.createMatch(
                    clubId = clubId,
                    userId = club.ownerId,
                    scheduledAt = scheduledAt,
                    signupOpensAt = signupOpensAt,
                    signupClosesAt = signupClosesAt
                )
            }.onFailure { logger.warn("Failed to create weekly match for club ${club.id}", it) }
        }
    }

    @Scheduled(cron = "0 */15 * * * *")
    fun autoGenerateTeamsForClosedSignups() {
        val matches = clubMatchRepository.findAllByStatusAndSignupClosesAtLessThanEqual(
            status = MatchStatusEntity.SCHEDULED,
            signupClosesAt = Instant.now()
        )

        matches.forEach { match ->
            runCatching {
                val club = clubRepository.findByIdOrNull(match.clubId) ?: return@runCatching
                clubService.generateTeams(
                    matchId = requireNotNull(match.id),
                    userId = club.ownerId,
                    mode = AUTO,
                    manualTeamA = null,
                    manualTeamB = null
                )
            }.onFailure { logger.warn("Failed to auto-generate teams for match ${match.id}", it) }
        }
    }
}
