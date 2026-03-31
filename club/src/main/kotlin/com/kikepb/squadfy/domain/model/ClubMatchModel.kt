package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMatchId
import com.kikepb.squadfy.domain.type.ClubMemberId
import java.time.Instant

data class ClubMatchModel(
    val id: ClubMatchId,
    val clubId: ClubId,
    val scheduledAt: Instant,
    val teamAScore: Int?,
    val teamBScore: Int?,
    val teamA: List<ClubMemberId>,
    val teamB: List<ClubMemberId>,
    val createdAt: Instant,
    val updatedAt: Instant
)
