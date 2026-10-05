package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.exception.MatchAnnouncementAlreadyEnrolledException
import com.kikepb.squadfy.domain.exception.MatchAnnouncementClosedException
import com.kikepb.squadfy.domain.exception.MatchAnnouncementEntryNotFoundException
import com.kikepb.squadfy.domain.exception.MatchAnnouncementFullException
import com.kikepb.squadfy.domain.exception.MatchAnnouncementNotFoundException
import com.kikepb.squadfy.domain.exception.NotClubMemberException
import com.kikepb.squadfy.domain.model.MatchAnnouncementModel
import com.kikepb.squadfy.domain.model.MatchAnnouncementModel.MatchAnnouncementStatus.CLOSED
import com.kikepb.squadfy.domain.model.MatchAnnouncementModel.MatchAnnouncementStatus.OPEN
import com.kikepb.squadfy.domain.type.MatchAnnouncementId
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.MatchAnnouncementEntity
import com.kikepb.squadfy.infrastructure.database.entities.MatchAnnouncementEntryEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toMatchAnnouncementModel
import com.kikepb.squadfy.infrastructure.database.repositories.MatchAnnouncementEntryRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MatchAnnouncementRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class MatchAnnouncementService(
    private val matchAnnouncementRepository: MatchAnnouncementRepository,
    private val matchAnnouncementEntryRepository: MatchAnnouncementEntryRepository,
    private val jdbcTemplate: JdbcTemplate
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

    fun getMatchAnnouncementById(matchAnnouncementId: MatchAnnouncementId): MatchAnnouncementModel {
        val matchAnnouncement = matchAnnouncementRepository.findByIdOrNull(matchAnnouncementId)
            ?: throw MatchAnnouncementNotFoundException()
        val entries = matchAnnouncementEntryRepository.findAllByMatchAnnouncementId(matchAnnouncementId = matchAnnouncementId)
        return matchAnnouncement.toMatchAnnouncementModel(entries = entries)
    }

    fun getMatchAnnouncementByMatch(matchId: MatchId): MatchAnnouncementModel {
        val matchAnnouncement = matchAnnouncementRepository.findByMatchId(matchId = matchId)
            ?: throw MatchAnnouncementNotFoundException()
        val entries = matchAnnouncementEntryRepository.findAllByMatchAnnouncementId(matchAnnouncementId = requireNotNull(matchAnnouncement.id))
        return matchAnnouncement.toMatchAnnouncementModel(entries = entries)
    }

    fun getMatchAnnouncementsByClub(clubId: ClubId): List<MatchAnnouncementModel> {
        val matchAnnouncement = matchAnnouncementRepository.findAllByClubIdOrderByCreatedAtDesc(clubId = clubId)
        return matchAnnouncement.map { matchAnnouncement ->
            val entries = matchAnnouncementEntryRepository.findAllByMatchAnnouncementId(matchAnnouncementId = requireNotNull(matchAnnouncement.id))
            matchAnnouncement.toMatchAnnouncementModel(entries = entries)
        }
    }

    @Transactional
    fun enroll(matchAnnouncementId: MatchAnnouncementId, userId: UserId): MatchAnnouncementModel {
        val matchAnnouncement = matchAnnouncementRepository.findByIdOrNull(matchAnnouncementId)
            ?: throw MatchAnnouncementNotFoundException()

        if (matchAnnouncement.status != OPEN) throw MatchAnnouncementClosedException()

        val clubMemberId = findClubMemberId(clubId = matchAnnouncement.clubId, userId = userId)
            ?: throw NotClubMemberException()

        if (matchAnnouncementEntryRepository.existsByMatchAnnouncementIdAndClubMemberId(
                matchAnnouncementId = requireNotNull(matchAnnouncement.id),
                clubMemberId = clubMemberId
            )
        ) throw MatchAnnouncementAlreadyEnrolledException()

        val currentCount = matchAnnouncementEntryRepository.countByMatchAnnouncementId(matchAnnouncementId = requireNotNull(matchAnnouncement.id))
        if (currentCount >= matchAnnouncement.maxPlayers) throw MatchAnnouncementFullException()

        matchAnnouncementEntryRepository.saveAndFlush(
            MatchAnnouncementEntryEntity(
                matchAnnouncementId = requireNotNull(matchAnnouncement.id),
                clubMemberId = clubMemberId
            )
        )

        val entries = matchAnnouncementEntryRepository.findAllByMatchAnnouncementId(matchAnnouncementId = requireNotNull(matchAnnouncement.id))
        return matchAnnouncement.toMatchAnnouncementModel(entries = entries)
    }

    @Transactional
    fun withdraw(matchAnnouncementId: MatchAnnouncementId, userId: UserId): MatchAnnouncementModel {
        val matchAnnouncement = matchAnnouncementRepository.findByIdOrNull(matchAnnouncementId)
            ?: throw MatchAnnouncementNotFoundException()

        if (matchAnnouncement.status != OPEN) throw MatchAnnouncementClosedException()

        val clubMemberId = findClubMemberId(clubId = matchAnnouncement.clubId, userId = userId)
            ?: throw NotClubMemberException()

        if (!matchAnnouncementEntryRepository.existsByMatchAnnouncementIdAndClubMemberId(
                matchAnnouncementId = requireNotNull(matchAnnouncement.id),
                clubMemberId = clubMemberId
            )
        ) throw MatchAnnouncementEntryNotFoundException()

        matchAnnouncementEntryRepository.deleteByMatchAnnouncementIdAndClubMemberId(
            matchAnnouncementId = requireNotNull(matchAnnouncement.id),
            clubMemberId = clubMemberId
        )

        val entries = matchAnnouncementEntryRepository.findAllByMatchAnnouncementId(matchAnnouncementId = requireNotNull(matchAnnouncement.id))
        return matchAnnouncement.toMatchAnnouncementModel(entries = entries)
    }

    fun getEnrolledPlayersByMatch(matchId: MatchId): List<ClubMemberId> {
        val matchAnnouncement = matchAnnouncementRepository.findByMatchId(matchId = matchId) ?: return emptyList()
        return matchAnnouncementEntryRepository.findAllByMatchAnnouncementId(matchAnnouncementId = requireNotNull(matchAnnouncement.id))
            .map { it.clubMemberId }
    }

    @Transactional
    fun closeExpiredMatchAnnouncement(before: Instant) {
        val expired = matchAnnouncementRepository.findAllByStatusAndClosesAtBefore(
            status = OPEN,
            now = before
        )
        expired.forEach { it.status = CLOSED }
        matchAnnouncementRepository.saveAll(expired)
    }

    private fun findClubMemberId(clubId: ClubId, userId: UserId): ClubMemberId? {
        val sql = """
            SELECT id
            FROM club_service.club_members
            WHERE club_id = ? AND user_id = ?
        """.trimIndent()

        return jdbcTemplate.query(sql, { rs, _ ->
            rs.getObject("id", java.util.UUID::class.java)
        }, clubId, userId).firstOrNull()
    }
}
