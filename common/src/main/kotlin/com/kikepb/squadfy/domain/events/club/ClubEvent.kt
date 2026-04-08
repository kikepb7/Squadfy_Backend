package com.kikepb.squadfy.domain.events.club

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.kikepb.squadfy.domain.events.SquadfyEvent
import com.kikepb.squadfy.domain.events.user.InstantToStringSerializer
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.UserId
import java.time.Instant
import java.util.UUID

sealed class ClubEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val exchange: String = ClubEventConstants.CLUB_EXCHANGE,
    @JsonSerialize(using = InstantToStringSerializer::class)
    override val occurredAt: Instant = Instant.now()
) : SquadfyEvent {

    data class MemberJoined(
        val clubId: ClubId,
        val clubName: String,
        val newMemberUserId: UserId,
        val newMemberUsername: String,
        val adminUserIds: List<UserId>,
        override val eventKey: String = ClubEventConstants.CLUB_MEMBER_JOINED
    ) : ClubEvent(), SquadfyEvent

    data class MemberKicked(
        val clubId: ClubId,
        val clubName: String,
        val kickedUserId: UserId,
        val kickedUsername: String,
        override val eventKey: String = ClubEventConstants.CLUB_MEMBER_KICKED
    ) : ClubEvent(), SquadfyEvent

    data class JoinRequestReceived(
        val clubId: ClubId,
        val clubName: String,
        val requestingUserId: UserId,
        val requestingUsername: String,
        val adminUserIds: List<UserId>,
        override val eventKey: String = ClubEventConstants.CLUB_JOIN_REQUEST_RECEIVED
    ) : ClubEvent(), SquadfyEvent

    data class JoinRequestApproved(
        val clubId: ClubId,
        val clubName: String,
        val requestingUserId: UserId,
        override val eventKey: String = ClubEventConstants.CLUB_JOIN_REQUEST_APPROVED
    ) : ClubEvent(), SquadfyEvent

    data class JoinRequestRejected(
        val clubId: ClubId,
        val clubName: String,
        val requestingUserId: UserId,
        override val eventKey: String = ClubEventConstants.CLUB_JOIN_REQUEST_REJECTED
    ) : ClubEvent(), SquadfyEvent

    data class RankingChanged(
        val clubId: ClubId,
        val clubName: String,
        val memberUserIds: List<UserId>,
        override val eventKey: String = ClubEventConstants.CLUB_RANKING_CHANGED
    ) : ClubEvent(), SquadfyEvent
}
