package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMatchId
import com.kikepb.squadfy.domain.type.ClubMemberId
import java.time.Instant

data class ClubMatchDto(
    val id: ClubMatchId,
    val clubId: ClubId,
    val scheduledAt: Instant,
    val teamA: List<ClubMemberId>,
    val teamB: List<ClubMemberId>,
    val createdAt: Instant,
    val updatedAt: Instant
)
