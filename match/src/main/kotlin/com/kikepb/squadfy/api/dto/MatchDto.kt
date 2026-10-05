package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchId
import java.time.Instant

data class MatchDto(
    val id: MatchId,
    val clubId: ClubId,
    val scheduledAt: Instant,
    val status: MatchStatus,
    val enrolledPlayers: List<ClubMemberId>,
    val teamA: List<ClubMemberId>,
    val teamB: List<ClubMemberId>,
    val teamAScore: Int,
    val teamBScore: Int,
    val goals: List<MatchEventDto>,
    val assists: List<MatchEventDto>,
    val yellowCards: List<MatchEventDto>,
    val redCards: List<MatchEventDto>,
    val createdAt: Instant,
    val updatedAt: Instant
)
