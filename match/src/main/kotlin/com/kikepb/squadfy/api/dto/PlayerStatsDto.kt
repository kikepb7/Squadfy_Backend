package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.type.ClubMemberId

data class PlayerStatsDto(
    val clubMemberId: ClubMemberId,
    val matchesPlayed: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val goals: Int,
    val assists: Int,
    val yellowCards: Int,
    val redCards: Int,
    val minutesPlayed: Int
)

/** Row of the club's statistics classification. Name and photo come from GET /api/v1/clubs/{clubId}/members. */
data class ClubStatsEntryDto(
    val rank: Int,
    val clubMemberId: ClubMemberId,
    val matchesPlayed: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val goals: Int,
    val assists: Int,
    val yellowCards: Int,
    val redCards: Int,
    val minutesPlayed: Int
)
