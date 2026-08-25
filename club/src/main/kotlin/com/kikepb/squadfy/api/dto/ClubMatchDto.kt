package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.model.MatchStatusModel
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMatchId
import java.time.Instant

data class ClubMatchDto(
    val id: ClubMatchId,
    val clubId: ClubId,
    val scheduledAt: Instant,
    val signupOpensAt: Instant,
    val signupClosesAt: Instant,
    val status: MatchStatusModel,
    val teamAScore: Int?,
    val teamBScore: Int?,
    val teamA: List<MatchParticipantDto>,
    val teamB: List<MatchParticipantDto>,
    val createdAt: Instant,
    val updatedAt: Instant
)
