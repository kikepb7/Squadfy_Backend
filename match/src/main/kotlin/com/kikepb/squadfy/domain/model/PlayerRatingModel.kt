package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId

data class PlayerRatingModel(
    val clubId: ClubId,
    val clubMemberId: ClubMemberId,
    val rating: Double,
    val matchesRated: Int,
    /** Position in the club classification (ties share the position). */
    val rank: Int,
    val totalPlayers: Int
) {
    /** The rating still moves fast and is not reliable until the player has some matches. */
    val isProvisional: Boolean get() = matchesRated < PlayerRatingCalculator.PROVISIONAL_MATCHES
}
