package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.ClubMemberId

/** Statistics of a member over the club's completed matches (spec 004 RN-7). */
data class PlayerStatsModel(
    val clubMemberId: ClubMemberId,
    val matchesPlayed: Int = 0,
    val wins: Int = 0,
    val draws: Int = 0,
    val losses: Int = 0,
    val goals: Int = 0,
    val assists: Int = 0,
    val yellowCards: Int = 0,
    val redCards: Int = 0,
    val minutesPlayed: Int = 0
)
