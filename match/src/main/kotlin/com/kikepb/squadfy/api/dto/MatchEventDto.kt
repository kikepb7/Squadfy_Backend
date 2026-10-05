package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.model.MatchEventType
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchEventId
import com.kikepb.squadfy.domain.type.MatchId
import java.time.Instant

data class MatchEventDto(
    val id: MatchEventId,
    val matchId: MatchId,
    val clubMemberId: ClubMemberId,
    val type: MatchEventType,
    val minute: Int?,
    val createdAt: Instant
)
