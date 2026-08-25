package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMatchId
import java.time.Instant

data class ClubMatchModel(
    val id: ClubMatchId,
    val clubId: ClubId,
    val scheduledAt: Instant,
    val signupOpensAt: Instant,
    val signupClosesAt: Instant,
    val status: MatchStatusModel,
    val teamAScore: Int?,
    val teamBScore: Int?,
    val teamA: List<MatchParticipantModel>,
    val teamB: List<MatchParticipantModel>,
    val createdAt: Instant,
    val updatedAt: Instant
)
