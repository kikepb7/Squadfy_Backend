package com.kikepb.squadfy.api.dto.websocket

import com.kikepb.squadfy.domain.events.live.ClubDataChangedEvent.Scope
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.MatchId

/** What to reload: the match ([scope] MATCH, with [matchId]), the schedule or the absences of the club. */
data class ClubDataChangedDto(
    val clubId: ClubId,
    val scope: Scope,
    val matchId: MatchId?
)
