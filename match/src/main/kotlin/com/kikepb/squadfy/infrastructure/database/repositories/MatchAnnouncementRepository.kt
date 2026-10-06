package com.kikepb.squadfy.infrastructure.database.repositories

import com.kikepb.squadfy.domain.model.MatchAnnouncementModel.MatchAnnouncementStatus
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.MatchAnnouncementId
import com.kikepb.squadfy.domain.type.MatchId
import com.kikepb.squadfy.infrastructure.database.entities.MatchAnnouncementEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface MatchAnnouncementRepository : JpaRepository<MatchAnnouncementEntity, MatchAnnouncementId> {

    fun findByMatchId(matchId: MatchId): MatchAnnouncementEntity?

    fun findAllByMatchIdIn(matchIds: Collection<MatchId>): List<MatchAnnouncementEntity>

    fun findAllByClubIdOrderByCreatedAtDesc(clubId: ClubId): List<MatchAnnouncementEntity>

    fun findAllByClubIdAndStatusAndClosesAtAfter(clubId: ClubId, status: MatchAnnouncementStatus, now: Instant): List<MatchAnnouncementEntity>

    fun findAllByStatusAndOpensAtLessThanEqualAndOpenedNotifiedAtIsNull(status: MatchAnnouncementStatus, now: Instant): List<MatchAnnouncementEntity>

    fun findAllByStatusAndClosesAtAfterAndClosingReminderSentAtIsNull(status: MatchAnnouncementStatus, now: Instant): List<MatchAnnouncementEntity>

    fun findAllByStatusAndClosesAtLessThanEqual(status: MatchAnnouncementStatus, now: Instant): List<MatchAnnouncementEntity>

    /** Serializes enroll/withdraw on the same announcement so maxPlayers cannot be exceeded. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM MatchAnnouncementEntity a WHERE a.id = :id")
    fun findByIdForUpdate(id: MatchAnnouncementId): MatchAnnouncementEntity?
}
