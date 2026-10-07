package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.exception.MatchAnnouncementAlreadyEnrolledException
import com.kikepb.squadfy.domain.exception.MatchAnnouncementClosedException
import com.kikepb.squadfy.domain.exception.MatchAnnouncementEntryNotFoundException
import com.kikepb.squadfy.domain.exception.GuestNotFoundException
import com.kikepb.squadfy.domain.exception.MatchAnnouncementNotFoundException
import com.kikepb.squadfy.domain.exception.TooManyGuestsException
import com.kikepb.squadfy.domain.exception.ForbiddenException
import com.kikepb.squadfy.domain.model.MatchAnnouncementEntryModel.EntryStatus.CONFIRMED
import com.kikepb.squadfy.domain.model.MatchAnnouncementEntryModel.EntryStatus.WAITLISTED
import com.kikepb.squadfy.domain.club.PlayerPosition
import com.kikepb.squadfy.domain.model.CurrentMatchAnnouncementModel
import com.kikepb.squadfy.domain.model.EnrollmentAllocator
import com.kikepb.squadfy.domain.model.MatchAnnouncementEntryModel.ParticipantType
import com.kikepb.squadfy.domain.model.MatchGuestModel
import com.kikepb.squadfy.domain.model.MatchAnnouncementModel
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus
import com.kikepb.squadfy.domain.model.MatchAnnouncementModel.MatchAnnouncementStatus.CANCELLED
import com.kikepb.squadfy.domain.model.MatchAnnouncementModel.MatchAnnouncementStatus.CLOSED
import com.kikepb.squadfy.domain.model.MatchAnnouncementModel.MatchAnnouncementStatus.OPEN
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchAnnouncementId
import com.kikepb.squadfy.domain.type.MatchId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.MatchAnnouncementEntity
import com.kikepb.squadfy.infrastructure.database.entities.MatchAnnouncementEntryEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toMatchAnnouncementModel
import com.kikepb.squadfy.infrastructure.database.mappers.toMatchGuestModel
import com.kikepb.squadfy.infrastructure.database.repositories.MatchAnnouncementEntryRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MatchAnnouncementRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MatchRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class MatchAnnouncementService(
    private val matchAnnouncementRepository: MatchAnnouncementRepository,
    private val matchAnnouncementEntryRepository: MatchAnnouncementEntryRepository,
    private val matchRepository: MatchRepository,
    private val matchNotificationPublisher: MatchNotificationPublisher,
    private val clubAccessGuard: ClubAccessGuard,
    private val clock: Clock
) {

    @Transactional
    fun createMatchAnnouncement(
        matchId: MatchId,
        clubId: ClubId,
        maxPlayers: Int,
        opensAt: Instant,
        closesAt: Instant,
        drawAt: Instant = closesAt
    ): MatchAnnouncementModel {
        val entity = matchAnnouncementRepository.saveAndFlush(
            MatchAnnouncementEntity(
                matchId = matchId,
                clubId = clubId,
                maxPlayers = maxPlayers,
                opensAt = opensAt,
                closesAt = closesAt,
                drawAt = drawAt
            )
        )
        return entity.toMatchAnnouncementModel(entries = emptyList())
    }

    fun getMatchAnnouncementById(matchAnnouncementId: MatchAnnouncementId, userId: UserId): MatchAnnouncementModel {
        val matchAnnouncement = matchAnnouncementRepository.findByIdOrNull(matchAnnouncementId)
            ?: throw MatchAnnouncementNotFoundException()
        clubAccessGuard.requireMember(clubId = matchAnnouncement.clubId, userId = userId)
        return matchAnnouncement.withEntries()
    }

    fun getMatchAnnouncementByMatch(matchId: MatchId, userId: UserId): MatchAnnouncementModel {
        val matchAnnouncement = matchAnnouncementRepository.findByMatchId(matchId = matchId)
            ?: throw MatchAnnouncementNotFoundException()
        clubAccessGuard.requireMember(clubId = matchAnnouncement.clubId, userId = userId)
        return matchAnnouncement.withEntries()
    }

    fun getMatchAnnouncementsByClub(clubId: ClubId, userId: UserId): List<MatchAnnouncementModel> {
        clubAccessGuard.requireMember(clubId = clubId, userId = userId)
        val announcements = matchAnnouncementRepository.findAllByClubIdOrderByCreatedAtDesc(clubId = clubId)
        if (announcements.isEmpty()) return emptyList()
        val entriesByAnnouncement = matchAnnouncementEntryRepository
            .findAllByMatchAnnouncementIdIn(matchAnnouncementIds = announcements.map { requireNotNull(it.id) })
            .groupBy { it.matchAnnouncementId }

        return announcements.map { announcement ->
            announcement.toMatchAnnouncementModel(
                entries = entriesByAnnouncement[announcement.id].orEmpty().sortedBy { it.enrolledAt }
            )
        }
    }

    /** Announcement of the club's next scheduled match with the requester's enrollment status. */
    fun getCurrentForClub(clubId: ClubId, userId: UserId): CurrentMatchAnnouncementModel {
        val memberId = clubAccessGuard.requireMember(clubId = clubId, userId = userId).memberId
        val nextMatch = matchRepository.findFirstByClubIdAndStatusAndScheduledAtAfterOrderByScheduledAtAsc(
            clubId = clubId,
            status = MatchStatus.SCHEDULED,
            after = clock.instant()
        ) ?: throw MatchAnnouncementNotFoundException()

        val announcement = matchAnnouncementRepository.findByMatchId(matchId = requireNotNull(nextMatch.id))
            ?: throw MatchAnnouncementNotFoundException()

        return CurrentMatchAnnouncementModel.of(
            announcement = announcement.withEntries(),
            matchScheduledAt = nextMatch.scheduledAt,
            memberId = memberId
        )
    }

    @Transactional
    fun enroll(matchAnnouncementId: MatchAnnouncementId, userId: UserId): MatchAnnouncementModel {
        val matchAnnouncement = lockAnnouncement(matchAnnouncementId)
        val announcementId = requireNotNull(matchAnnouncement.id)

        val clubMemberId = clubAccessGuard.requireMember(clubId = matchAnnouncement.clubId, userId = userId).memberId
        ensureOpen(matchAnnouncement)

        if (matchAnnouncementEntryRepository.existsByMatchAnnouncementIdAndClubMemberId(
                matchAnnouncementId = announcementId,
                clubMemberId = clubMemberId
            )
        ) throw MatchAnnouncementAlreadyEnrolledException()

        val entry = matchAnnouncementEntryRepository.saveAndFlush(
            MatchAnnouncementEntryEntity(matchAnnouncementId = announcementId, clubMemberId = clubMemberId, status = WAITLISTED)
        )
        reallocate(announcement = matchAnnouncement, newEntryId = entry.id)
        return matchAnnouncement.withEntries()
    }

    @Transactional
    fun withdraw(matchAnnouncementId: MatchAnnouncementId, userId: UserId): MatchAnnouncementModel {
        val matchAnnouncement = lockAnnouncement(matchAnnouncementId)

        val clubMemberId = clubAccessGuard.requireMember(clubId = matchAnnouncement.clubId, userId = userId).memberId
        ensureOpen(matchAnnouncement)

        val entry = matchAnnouncementEntryRepository.findByMatchAnnouncementIdAndClubMemberId(
            matchAnnouncementId = requireNotNull(matchAnnouncement.id),
            clubMemberId = clubMemberId
        ) ?: throw MatchAnnouncementEntryNotFoundException()

        removeEntries(announcement = matchAnnouncement, entries = listOf(entry))
        return matchAnnouncement.withEntries()
    }

    /** A member adds a guest; members keep priority over guests for the places (spec 008 RN-A). */
    @Transactional
    fun addGuest(matchAnnouncementId: MatchAnnouncementId, userId: UserId, name: String, position: PlayerPosition?): MatchAnnouncementModel {
        val matchAnnouncement = lockAnnouncement(matchAnnouncementId)
        val announcementId = requireNotNull(matchAnnouncement.id)

        val host = clubAccessGuard.requireMember(clubId = matchAnnouncement.clubId, userId = userId)
        ensureOpen(matchAnnouncement)
        if (matchAnnouncementEntryRepository.countByMatchAnnouncementIdAndInvitedByMemberId(announcementId, host.memberId) >= MAX_GUESTS_PER_MEMBER) {
            throw TooManyGuestsException()
        }

        val entry = matchAnnouncementEntryRepository.saveAndFlush(
            MatchAnnouncementEntryEntity(
                matchAnnouncementId = announcementId,
                participantType = ParticipantType.GUEST,
                guestName = name.trim(),
                guestPosition = position?.name,
                invitedByMemberId = host.memberId,
                status = WAITLISTED
            )
        )
        reallocate(announcement = matchAnnouncement, newEntryId = entry.id)
        return matchAnnouncement.withEntries()
    }

    /** The host of the guest or a manager removes a guest. */
    @Transactional
    fun removeGuest(matchAnnouncementId: MatchAnnouncementId, userId: UserId, guestId: UUID): MatchAnnouncementModel {
        val matchAnnouncement = lockAnnouncement(matchAnnouncementId)
        val requester = clubAccessGuard.requireMember(clubId = matchAnnouncement.clubId, userId = userId)
        ensureOpen(matchAnnouncement)

        val guest = matchAnnouncementEntryRepository.findByIdOrNull(guestId)
            ?.takeIf { it.matchAnnouncementId == matchAnnouncement.id && it.participantType == ParticipantType.GUEST }
            ?: throw GuestNotFoundException()
        if (guest.invitedByMemberId != requester.memberId && !requester.role.canManageClub) throw ForbiddenException()

        removeEntries(announcement = matchAnnouncement, entries = listOf(guest))
        return matchAnnouncement.withEntries()
    }

    /**
     * A member who left the club (or was removed) is taken out of every open announcement of that
     * club together with their guests (spec 001 RN-10, spec 008 RN-A5). Closed announcements are kept.
     */
    @Transactional
    fun withdrawFromOpenAnnouncements(clubId: ClubId, clubMemberId: ClubMemberId) {
        openAnnouncements(clubId = clubId).forEach { announcement ->
            val announcementId = requireNotNull(announcement.id)
            val entries = listOfNotNull(
                matchAnnouncementEntryRepository.findByMatchAnnouncementIdAndClubMemberId(announcementId, clubMemberId)
            ) + matchAnnouncementEntryRepository.findAllByMatchAnnouncementIdAndInvitedByMemberId(announcementId, clubMemberId)
            if (entries.isNotEmpty()) removeEntries(announcement = announcement, entries = entries)
        }
    }

    /** Withdraws the member from open announcements of matches whose local date is in [dates] (absence, RN-C2). */
    @Transactional
    fun withdrawForAbsence(clubId: ClubId, clubMemberId: ClubMemberId, matchIdsInAbsence: Collection<MatchId>) {
        openAnnouncements(clubId = clubId)
            .filter { it.matchId in matchIdsInAbsence }
            .forEach { announcement ->
                val entry = matchAnnouncementEntryRepository.findByMatchAnnouncementIdAndClubMemberId(
                    matchAnnouncementId = requireNotNull(announcement.id),
                    clubMemberId = clubMemberId
                ) ?: return@forEach
                removeEntries(announcement = announcement, entries = listOf(entry))
            }
    }

    /** Every enrolled member (confirmed and waitlisted) of the match's announcement; guests are excluded. */
    fun getAllEntriesByMatch(matchId: MatchId): List<ClubMemberId> {
        val announcement = matchAnnouncementRepository.findByMatchId(matchId = matchId) ?: return emptyList()
        return matchAnnouncementEntryRepository.findAllByMatchAnnouncementIdOrderByEnrolledAtAsc(
            matchAnnouncementId = requireNotNull(announcement.id)
        ).mapNotNull { it.clubMemberId }
    }

    /** Confirmed members of each match, in enrollment order. */
    fun getEnrolledPlayersByMatch(matchId: MatchId): List<ClubMemberId> =
        getEnrolledPlayersByMatches(matchIds = listOf(matchId))[matchId].orEmpty()

    fun getEnrolledPlayersByMatches(matchIds: Collection<MatchId>): Map<MatchId, List<ClubMemberId>> =
        confirmedEntriesByMatch(matchIds = matchIds).mapValues { (_, entries) -> entries.mapNotNull { it.clubMemberId } }

    /** Confirmed guests of each match, in the order they were added. */
    fun getConfirmedGuestsByMatches(matchIds: Collection<MatchId>): Map<MatchId, List<MatchAnnouncementEntryEntity>> =
        confirmedEntriesByMatch(matchIds = matchIds).mapValues { (_, entries) -> entries.filter { it.participantType == ParticipantType.GUEST } }

    fun getConfirmedGuestsByMatch(matchId: MatchId): List<MatchGuestModel> =
        getConfirmedGuestsByMatches(matchIds = listOf(matchId))[matchId].orEmpty().map { it.toMatchGuestModel() }

    fun findGuestEntries(guestIds: Collection<UUID>): Map<UUID, MatchAnnouncementEntryEntity> =
        if (guestIds.isEmpty()) emptyMap() else matchAnnouncementEntryRepository.findAllById(guestIds).associateBy { requireNotNull(it.id) }

    @Transactional
    fun cancelForMatch(matchId: MatchId) {
        matchAnnouncementRepository.findByMatchId(matchId = matchId)?.let { it.status = CANCELLED }
    }

    /** Brings back the announcement of a reactivated match: open again unless its close time passed. */
    @Transactional
    fun reactivateForMatch(matchId: MatchId) {
        val announcement = matchAnnouncementRepository.findByMatchId(matchId = matchId) ?: return
        announcement.status = if (announcement.closesAt.isAfter(clock.instant())) OPEN else CLOSED
    }

    /**
     * New close and draw times after the match was moved (spec 008 RN-B3). A closed announcement
     * opens again if the new close time is in the future, and pending reminders and draws are reset.
     */
    @Transactional
    fun updateDeadlines(matchId: MatchId, closesAt: Instant, drawAt: Instant) {
        val announcement = matchAnnouncementRepository.findByMatchId(matchId = matchId) ?: return
        val now = clock.instant()
        if (announcement.closesAt != closesAt) announcement.closingReminderSentAt = null
        announcement.closesAt = closesAt
        announcement.drawAt = drawAt
        if (announcement.opensAt.isAfter(closesAt)) announcement.opensAt = now.coerceAtMost(closesAt)
        if (announcement.status == CLOSED && closesAt.isAfter(now)) announcement.status = OPEN
        if (drawAt.isAfter(now)) announcement.teamsPublishedAt = null
    }

    /** @return ids of the matches whose announcement has just been closed. */
    @Transactional
    fun closeExpiredMatchAnnouncements(): List<MatchId> {
        val expired = matchAnnouncementRepository.findAllByStatusAndClosesAtLessThanEqual(
            status = OPEN,
            now = clock.instant()
        )
        expired.forEach { it.status = CLOSED }
        return expired.map { it.matchId }
    }

    /** Closed announcements whose draw time arrived and whose teams were not published yet (spec 008 RN-D3). */
    fun findDueDraws(): List<MatchId> =
        matchAnnouncementRepository.findAllByStatusAndDrawAtLessThanEqualAndTeamsPublishedAtIsNull(status = CLOSED, now = clock.instant())
            .map { it.matchId }

    @Transactional
    fun markTeamsPublished(matchId: MatchId) {
        matchAnnouncementRepository.findByMatchId(matchId = matchId)?.let { it.teamsPublishedAt = clock.instant() }
    }

    private fun lockAnnouncement(matchAnnouncementId: MatchAnnouncementId): MatchAnnouncementEntity =
        matchAnnouncementRepository.findByIdForUpdate(id = matchAnnouncementId) ?: throw MatchAnnouncementNotFoundException()

    private fun openAnnouncements(clubId: ClubId): List<MatchAnnouncementEntity> =
        matchAnnouncementRepository.findAllByClubIdAndStatusAndClosesAtAfter(clubId = clubId, status = OPEN, now = clock.instant())
            .mapNotNull { matchAnnouncementRepository.findByIdForUpdate(id = requireNotNull(it.id)) }

    private fun removeEntries(announcement: MatchAnnouncementEntity, entries: List<MatchAnnouncementEntryEntity>) {
        matchAnnouncementEntryRepository.deleteAll(entries)
        matchAnnouncementEntryRepository.flush()
        reallocate(announcement = announcement, newEntryId = null)
    }

    /**
     * Recomputes confirmed/waitlisted places with [EnrollmentAllocator] and notifies the members
     * that moved from the waitlist to a confirmed place (the just-created entry is not a promotion).
     */
    private fun reallocate(announcement: MatchAnnouncementEntity, newEntryId: UUID?) {
        val entries = matchAnnouncementEntryRepository.findAllByMatchAnnouncementIdOrderByEnrolledAtAsc(
            matchAnnouncementId = requireNotNull(announcement.id)
        )
        val allocation = EnrollmentAllocator.allocate(
            participants = entries.map {
                EnrollmentAllocator.Participant(
                    entryId = requireNotNull(it.id),
                    isGuest = it.participantType == ParticipantType.GUEST,
                    enrolledAt = it.enrolledAt
                )
            },
            maxPlayers = announcement.maxPlayers
        )

        val promotedMembers = entries.filter { entry ->
            val newStatus = allocation.getValue(requireNotNull(entry.id))
            val promoted = entry.id != newEntryId && entry.status == WAITLISTED && newStatus == CONFIRMED && entry.clubMemberId != null
            entry.status = newStatus
            promoted
        }
        matchAnnouncementEntryRepository.saveAllAndFlush(entries)
        if (promotedMembers.isEmpty()) return

        val match = matchRepository.findByIdOrNull(announcement.matchId) ?: return
        promotedMembers.forEach { entry ->
            matchNotificationPublisher.promotedFromWaitlist(
                clubId = announcement.clubId,
                matchId = announcement.matchId,
                matchScheduledAt = match.scheduledAt,
                announcementId = requireNotNull(announcement.id),
                memberId = requireNotNull(entry.clubMemberId)
            )
        }
    }

    private fun confirmedEntriesByMatch(matchIds: Collection<MatchId>): Map<MatchId, List<MatchAnnouncementEntryEntity>> {
        if (matchIds.isEmpty()) return emptyMap()
        val announcements = matchAnnouncementRepository.findAllByMatchIdIn(matchIds = matchIds)
        val matchIdByAnnouncement = announcements.associate { requireNotNull(it.id) to it.matchId }
        if (matchIdByAnnouncement.isEmpty()) return emptyMap()

        return matchAnnouncementEntryRepository
            .findAllByMatchAnnouncementIdIn(matchAnnouncementIds = matchIdByAnnouncement.keys)
            .filter { it.status == CONFIRMED }
            .sortedBy { it.enrolledAt }
            .groupBy { matchIdByAnnouncement.getValue(it.matchAnnouncementId) }
    }

    private fun ensureOpen(matchAnnouncement: MatchAnnouncementEntity) {
        val now = clock.instant()
        when {
            matchAnnouncement.status != OPEN || !now.isBefore(matchAnnouncement.closesAt) ->
                throw MatchAnnouncementClosedException()
            now.isBefore(matchAnnouncement.opensAt) ->
                throw MatchAnnouncementClosedException("The match announcement opens at ${matchAnnouncement.opensAt}")
        }
    }

    private fun MatchAnnouncementEntity.withEntries(): MatchAnnouncementModel =
        toMatchAnnouncementModel(
            entries = matchAnnouncementEntryRepository.findAllByMatchAnnouncementIdOrderByEnrolledAtAsc(
                matchAnnouncementId = requireNotNull(id)
            )
        )

    private companion object {
        const val MAX_GUESTS_PER_MEMBER = 2
    }
}
