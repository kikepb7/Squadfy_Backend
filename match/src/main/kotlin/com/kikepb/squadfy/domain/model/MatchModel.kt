package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchId
import java.time.Instant

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
    val createdAt: Instant,
    val updatedAt: Instant
) {
    val teamAScore: Int get() = events.count { it.type == MatchEventType.GOAL && it.clubMemberId in teamA }
    val teamBScore: Int get() = events.count { it.type == MatchEventType.GOAL && it.clubMemberId in teamB }

    enum class MatchStatus {
        SCHEDULED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
}
