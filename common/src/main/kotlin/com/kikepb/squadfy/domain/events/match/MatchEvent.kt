package com.kikepb.squadfy.domain.events.match

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.kikepb.squadfy.domain.events.SquadfyEvent
import com.kikepb.squadfy.domain.events.user.InstantToStringSerializer
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.MatchAnnouncementId
import com.kikepb.squadfy.domain.type.MatchId
import com.kikepb.squadfy.domain.type.UserId
import java.time.Instant
import java.util.UUID

/**
 * Match cycle notifications (spec 005). `match` resolves the recipients; `notification` filters
 * muted clubs and sends the push. Every event carries what is needed to write the message in the
 * club's time zone.
 */
sealed class MatchEvent(
    override val eventId: String = UUID.randomUUID().toString(),
    override val exchange: String = MatchEventConstants.MATCH_EXCHANGE,
    @JsonSerialize(using = InstantToStringSerializer::class)
    override val occurredAt: Instant = Instant.now()
) : SquadfyEvent {

    abstract val clubId: ClubId
    abstract val clubName: String
    abstract val matchId: MatchId
    abstract val matchScheduledAt: Instant
    abstract val timeZone: String

    data class AnnouncementOpened(
        override val clubId: ClubId,
        override val clubName: String,
        override val matchId: MatchId,
        override val matchScheduledAt: Instant,
        override val timeZone: String,
        val announcementId: MatchAnnouncementId,
        val closesAt: Instant,
        val recipientUserIds: List<UserId>,
        override val eventKey: String = MatchEventConstants.ANNOUNCEMENT_OPENED
    ) : MatchEvent()

    data class AnnouncementClosingSoon(
        override val clubId: ClubId,
        override val clubName: String,
        override val matchId: MatchId,
        override val matchScheduledAt: Instant,
        override val timeZone: String,
        val announcementId: MatchAnnouncementId,
        val closesAt: Instant,
        val freePlaces: Int,
        val recipientUserIds: List<UserId>,
        override val eventKey: String = MatchEventConstants.ANNOUNCEMENT_CLOSING_SOON
    ) : MatchEvent()

    data class TeamsPublished(
        override val clubId: ClubId,
        override val clubName: String,
        override val matchId: MatchId,
        override val matchScheduledAt: Instant,
        override val timeZone: String,
        val teamAUserIds: List<UserId>,
        val teamBUserIds: List<UserId>,
        override val eventKey: String = MatchEventConstants.TEAMS_PUBLISHED
    ) : MatchEvent()

    data class MatchCancelled(
        override val clubId: ClubId,
        override val clubName: String,
        override val matchId: MatchId,
        override val matchScheduledAt: Instant,
        override val timeZone: String,
        val recipientUserIds: List<UserId>,
        override val eventKey: String = MatchEventConstants.MATCH_CANCELLED
    ) : MatchEvent()

    data class PromotedFromWaitlist(
        override val clubId: ClubId,
        override val clubName: String,
        override val matchId: MatchId,
        override val matchScheduledAt: Instant,
        override val timeZone: String,
        val announcementId: MatchAnnouncementId,
        val userId: UserId,
        override val eventKey: String = MatchEventConstants.WAITLIST_PROMOTED
    ) : MatchEvent()
}
