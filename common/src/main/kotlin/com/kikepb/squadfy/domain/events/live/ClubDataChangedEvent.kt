package com.kikepb.squadfy.domain.events.live

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.MatchId

/**
 * In-process notice that data of a club changed, so connected members can reload it (spec 012 RN-A).
 * Published by match after the commit and delivered by chat through the WebSocket of the instance.
 */
data class ClubDataChangedEvent(
    val clubId: ClubId,
    val scope: Scope,
    val matchId: MatchId? = null
) {
    enum class Scope {
        /** A match, its announcement, enrollments, teams, events or score. */
        MATCH,
        /** The weekly schedule or its exceptions. */
        SCHEDULE,
        /** Absences of the members. */
        ABSENCES
    }
}
