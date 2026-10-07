package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.club.PlayerPosition
import java.util.UUID

/** A guest player added by a member to an announcement (spec 008 RN-A). [guestId] is the entry id. */
data class MatchGuestModel(
    val guestId: UUID,
    val name: String,
    val position: PlayerPosition?,
    val invitedByMemberId: com.kikepb.squadfy.domain.type.ClubMemberId?
)
