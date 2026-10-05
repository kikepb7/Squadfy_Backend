package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchEventId
import com.kikepb.squadfy.domain.type.MatchId
import java.time.Instant

data class MatchEventModel(
    val id: MatchEventId,
    val matchId: MatchId,
    val clubMemberId: ClubMemberId,
    val type: MatchEventType,
    val minute: Int?,
    val createdAt: Instant
)

enum class MatchEventType {
    GOAL,
    ASSIST,
    YELLOW_CARD,
    RED_CARD
}
