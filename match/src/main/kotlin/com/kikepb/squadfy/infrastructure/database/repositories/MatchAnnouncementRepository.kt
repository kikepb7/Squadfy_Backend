package com.kikepb.squadfy.infrastructure.database.repositories

import com.kikepb.squadfy.domain.model.MatchAnnouncementModel.MatchAnnouncementStatus
import com.kikepb.squadfy.domain.type.MatchAnnouncementId
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.MatchId
import com.kikepb.squadfy.infrastructure.database.entities.MatchAnnouncementEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface MatchAnnouncementRepository : JpaRepository<MatchAnnouncementEntity, MatchAnnouncementId> {

    fun findByMatchId(matchId: MatchId): MatchAnnouncementEntity?

    fun findAllByClubIdOrderByCreatedAtDesc(clubId: ClubId): List<MatchAnnouncementEntity>

    fun existsByMatchId(matchId: MatchId): Boolean

    fun findAllByStatusAndClosesAtBefore(status: MatchAnnouncementStatus, now: Instant): List<MatchAnnouncementEntity>
}
