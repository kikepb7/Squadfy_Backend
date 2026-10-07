package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchId
import java.time.Instant
import java.time.LocalDate

data class MatchModel(
    val id: MatchId,
    val clubId: ClubId,
    val scheduledAt: Instant,
    val status: MatchStatus,
    val enrolledPlayers: List<ClubMemberId> = emptyList(),
    val teamA: List<ClubMemberId> = emptyList(),
    val teamB: List<ClubMemberId> = emptyList(),
    val events: List<MatchEventModel> = emptyList(),
    val durationMinutes: Int,
    /** Effective minutes of every team player (whole match unless a manager set them). */
    val minutesPlayed: Map<ClubMemberId, Int> = emptyMap(),
    /** Confirmed guests of the announcement (spec 008 RN-A). */
    val enrolledGuests: List<MatchGuestModel> = emptyList(),
    val teamAGuests: List<MatchGuestModel> = emptyList(),
    val teamBGuests: List<MatchGuestModel> = emptyList(),
    /** Manual final score; when set it is the official result (spec 008 RN-E2). */
    val manualTeamAScore: Int? = null,
    val manualTeamBScore: Int? = null,
    /** Rating variation of each member once the match is completed (spec 008 RN-F1). */
    val ratingChanges: Map<ClubMemberId, Double> = emptyMap(),
    val scheduleDate: LocalDate? = null,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    val isManualScore: Boolean get() = manualTeamAScore != null && manualTeamBScore != null
    val teamAScore: Int get() = if (isManualScore) manualTeamAScore!! else goalsOf(teamA)
    val teamBScore: Int get() = if (isManualScore) manualTeamBScore!! else goalsOf(teamB)

    private fun goalsOf(team: List<ClubMemberId>): Int =
        events.count { it.type == MatchEventType.GOAL && it.clubMemberId in team }

    enum class MatchStatus {
        SCHEDULED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
}
