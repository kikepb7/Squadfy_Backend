package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.exception.MatchAnnouncementAlreadyEnrolledException
import com.kikepb.squadfy.domain.exception.MatchAnnouncementClosedException
import com.kikepb.squadfy.domain.exception.MatchAnnouncementEntryNotFoundException
import com.kikepb.squadfy.domain.exception.MatchAnnouncementNotFoundException
import com.kikepb.squadfy.domain.model.MatchAnnouncementEntryModel.EntryStatus.CONFIRMED
import com.kikepb.squadfy.domain.model.MatchAnnouncementEntryModel.EntryStatus.WAITLISTED
import com.kikepb.squadfy.domain.model.MatchAnnouncementModel
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
import com.kikepb.squadfy.infrastructure.database.repositories.MatchAnnouncementEntryRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MatchAnnouncementRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class MatchAnnouncementService(
    private val matchAnnouncementRepository: MatchAnnouncementRepository,
    private val matchAnnouncementEntryRepository: MatchAnnouncementEntryRepository,
    private val clubAccessGuard: ClubAccessGuard,
    private val clock: Clock
) {

    @Transactional
    fun createMatchAnnouncement(
        matchId: MatchId,
        clubId: ClubId,
        maxPlayers: Int,
        opensAt: Instant,
        closesAt: Instant
    ): MatchAnnouncementModel {
        val entity = matchAnnouncementRepository.saveAndFlush(
            MatchAnnouncementEntity(
                matchId = matchId,
                clubId = clubId,
                maxPlayers = maxPlayers,
                opensAt = opensAt,
                closesAt = closesAt
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

    @Transactional
    fun enroll(matchAnnouncementId: MatchAnnouncementId, userId: UserId): MatchAnnouncementModel {
        val matchAnnouncement = matchAnnouncementRepository.findByIdForUpdate(id = matchAnnouncementId)
            ?: throw MatchAnnouncementNotFoundException()
        val announcementId = requireNotNull(matchAnnouncement.id)

        val clubMemberId = clubAccessGuard.requireMember(clubId = matchAnnouncement.clubId, userId = userId).memberId
        ensureOpen(matchAnnouncement)

        if (matchAnnouncementEntryRepository.existsByMatchAnnouncementIdAndClubMemberId(
                matchAnnouncementId = announcementId,
                clubMemberId = clubMemberId
            )
        ) throw MatchAnnouncementAlreadyEnrolledException()

        val confirmedCount = matchAnnouncementEntryRepository.countByMatchAnnouncementIdAndStatus(
            matchAnnouncementId = announcementId,
            status = CONFIRMED
        )

        matchAnnouncementEntryRepository.saveAndFlush(
            MatchAnnouncementEntryEntity(
                matchAnnouncementId = announcementId,
                clubMemberId = clubMemberId,
                status = if (confirmedCount < matchAnnouncement.maxPlayers) CONFIRMED else WAITLISTED
            )
        )

        return matchAnnouncement.withEntries()
    }

    @Transactional
    fun withdraw(matchAnnouncementId: MatchAnnouncementId, userId: UserId): MatchAnnouncementModel {
        val matchAnnouncement = matchAnnouncementRepository.findByIdForUpdate(id = matchAnnouncementId)
            ?: throw MatchAnnouncementNotFoundException()

        val clubMemberId = clubAccessGuard.requireMember(clubId = matchAnnouncement.clubId, userId = userId).memberId
        ensureOpen(matchAnnouncement)

        val entry = matchAnnouncementEntryRepository.findByMatchAnnouncementIdAndClubMemberId(
            matchAnnouncementId = requireNotNull(matchAnnouncement.id),
            clubMemberId = clubMemberId
        ) ?: throw MatchAnnouncementEntryNotFoundException()

        matchAnnouncementEntryRepository.delete(entry)
        matchAnnouncementEntryRepository.flush()

        if (entry.status == CONFIRMED) promoteFirstWaitlisted(matchAnnouncementId = entry.matchAnnouncementId)

        return matchAnnouncement.withEntries()
    }

    /** The first player on the waitlist takes the free confirmed place. */
    private fun promoteFirstWaitlisted(matchAnnouncementId: MatchAnnouncementId) {
        matchAnnouncementEntryRepository.findFirstByMatchAnnouncementIdAndStatusOrderByEnrolledAtAsc(
            matchAnnouncementId = matchAnnouncementId,
            status = WAITLISTED
        )?.let { it.status = CONFIRMED }
    }

    /** Confirmed players of each match, in enrollment order. */
    fun getEnrolledPlayersByMatch(matchId: MatchId): List<ClubMemberId> =
        getEnrolledPlayersByMatches(matchIds = listOf(matchId))[matchId].orEmpty()

    fun getEnrolledPlayersByMatches(matchIds: Collection<MatchId>): Map<MatchId, List<ClubMemberId>> {
        if (matchIds.isEmpty()) return emptyMap()
        val announcements = matchAnnouncementRepository.findAllByMatchIdIn(matchIds = matchIds)
        val matchIdByAnnouncement = announcements.associate { requireNotNull(it.id) to it.matchId }
        if (matchIdByAnnouncement.isEmpty()) return emptyMap()

        return matchAnnouncementEntryRepository
            .findAllByMatchAnnouncementIdIn(matchAnnouncementIds = matchIdByAnnouncement.keys)
            .filter { it.status == CONFIRMED }
            .sortedBy { it.enrolledAt }
            .groupBy(
                keySelector = { matchIdByAnnouncement.getValue(it.matchAnnouncementId) },
                valueTransform = { it.clubMemberId }
            )
    }

    @Transactional
    fun cancelForMatch(matchId: MatchId) {
        matchAnnouncementRepository.findByMatchId(matchId = matchId)?.let { it.status = CANCELLED }
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
}
