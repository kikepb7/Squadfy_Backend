package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.ClubMatchId
import com.kikepb.squadfy.domain.type.MatchPlayerStatId

data class MatchPlayerStatModel(
    val id: MatchPlayerStatId,
    val matchId: ClubMatchId,
    val clubMemberId: ClubMemberId,
    val goals: Int,
    val assists: Int,
    val yellowCards: Int,
    val redCards: Int,
    val minutesPlayed: Int
)
