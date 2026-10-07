package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.club.ClubMembershipProvider
import com.kikepb.squadfy.domain.events.match.MatchEvent
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchAnnouncementId
import com.kikepb.squadfy.domain.type.MatchId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.DEFAULT_CLUB_TIME_ZONE
import com.kikepb.squadfy.infrastructure.database.repositories.ClubMatchScheduleRepository
import com.kikepb.squadfy.infrastructure.message_queue.EventPublisher
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Builds the match cycle notification events (spec 005): resolves club members to users and adds
 * the club name and time zone. Events are published only if the surrounding transaction commits.
 * Depends on ports and repositories only, so any match service can use it without cycles.
 */
@Component
class MatchNotificationPublisher(
    private val clubMembershipProvider: ClubMembershipProvider,
    private val clubMatchScheduleRepository: ClubMatchScheduleRepository,
    private val eventPublisher: EventPublisher
) {

    /** [absentMemberIds]: members absent on the match date do not get the opening push (spec 008 RN-C3). */
    fun announcementOpened(
        clubId: ClubId,
        matchId: MatchId,
        matchScheduledAt: Instant,
        announcementId: MatchAnnouncementId,
        closesAt: Instant,
        absentMemberIds: Set<ClubMemberId> = emptySet()
    ) {
        val recipients = clubMembershipProvider.findAllMembers(clubId = clubId)
            .filterNot { it.memberId in absentMemberIds }
            .map { it.userId }
        if (recipients.isEmpty()) return

        eventPublisher.publishAfterCommit(
            MatchEvent.AnnouncementOpened(
                clubId = clubId,
                clubName = clubName(clubId),
                matchId = matchId,
                matchScheduledAt = matchScheduledAt,
                timeZone = timeZone(clubId),
                announcementId = announcementId,
                closesAt = closesAt,
                recipientUserIds = recipients
            )
        )
    }

    fun announcementClosingSoon(
        clubId: ClubId,
        matchId: MatchId,
        matchScheduledAt: Instant,
        announcementId: MatchAnnouncementId,
        closesAt: Instant,
        freePlaces: Int,
        enrolledMemberIds: Set<ClubMemberId>,
        absentMemberIds: Set<ClubMemberId> = emptySet()
    ) {
        val recipients = clubMembershipProvider.findAllMembers(clubId = clubId)
            .filterNot { it.memberId in enrolledMemberIds || it.memberId in absentMemberIds }
            .map { it.userId }
        if (recipients.isEmpty()) return

        eventPublisher.publishAfterCommit(
            MatchEvent.AnnouncementClosingSoon(
                clubId = clubId,
                clubName = clubName(clubId),
                matchId = matchId,
                matchScheduledAt = matchScheduledAt,
                timeZone = timeZone(clubId),
                announcementId = announcementId,
                closesAt = closesAt,
                freePlaces = freePlaces,
                recipientUserIds = recipients
            )
        )
    }

    fun teamsPublished(clubId: ClubId, matchId: MatchId, matchScheduledAt: Instant, teamA: List<ClubMemberId>, teamB: List<ClubMemberId>) {
        val users = userIdsOf(clubId = clubId, memberIds = teamA + teamB)
        eventPublisher.publishAfterCommit(
            MatchEvent.TeamsPublished(
                clubId = clubId,
                clubName = clubName(clubId),
                matchId = matchId,
                matchScheduledAt = matchScheduledAt,
                timeZone = timeZone(clubId),
                teamAUserIds = teamA.mapNotNull { users[it] },
                teamBUserIds = teamB.mapNotNull { users[it] }
            )
        )
    }

    fun matchCancelled(clubId: ClubId, matchId: MatchId, matchScheduledAt: Instant, enrolledMemberIds: List<ClubMemberId>) {
        val recipients = userIdsOf(clubId = clubId, memberIds = enrolledMemberIds).values.toList()
        if (recipients.isEmpty()) return

        eventPublisher.publishAfterCommit(
            MatchEvent.MatchCancelled(
                clubId = clubId,
                clubName = clubName(clubId),
                matchId = matchId,
                matchScheduledAt = matchScheduledAt,
                timeZone = timeZone(clubId),
                recipientUserIds = recipients
            )
        )
    }

    /** Every member of the club is told about the new date (spec 008 RN-B3). */
    fun matchRescheduled(clubId: ClubId, matchId: MatchId, previousScheduledAt: Instant, newScheduledAt: Instant) {
        val recipients = clubMembershipProvider.findAllMembers(clubId = clubId).map { it.userId }
        if (recipients.isEmpty()) return

        eventPublisher.publishAfterCommit(
            MatchEvent.MatchRescheduled(
                clubId = clubId,
                clubName = clubName(clubId),
                matchId = matchId,
                matchScheduledAt = newScheduledAt,
                timeZone = timeZone(clubId),
                previousScheduledAt = previousScheduledAt,
                recipientUserIds = recipients
            )
        )
    }

    fun promotedFromWaitlist(clubId: ClubId, matchId: MatchId, matchScheduledAt: Instant, announcementId: MatchAnnouncementId, memberId: ClubMemberId) {
        val userId = userIdsOf(clubId = clubId, memberIds = listOf(memberId))[memberId] ?: return

        eventPublisher.publishAfterCommit(
            MatchEvent.PromotedFromWaitlist(
                clubId = clubId,
                clubName = clubName(clubId),
                matchId = matchId,
                matchScheduledAt = matchScheduledAt,
                timeZone = timeZone(clubId),
                announcementId = announcementId,
                userId = userId
            )
        )
    }

    private fun userIdsOf(clubId: ClubId, memberIds: Collection<ClubMemberId>): Map<ClubMemberId, UserId> =
        clubMembershipProvider.findMembers(clubId = clubId, memberIds = memberIds).associate { it.memberId to it.userId }

    private fun clubName(clubId: ClubId): String = clubMembershipProvider.findClubName(clubId = clubId).orEmpty()

    private fun timeZone(clubId: ClubId): String =
        clubMatchScheduleRepository.findByClubId(clubId = clubId)?.timeZone ?: DEFAULT_CLUB_TIME_ZONE
}
