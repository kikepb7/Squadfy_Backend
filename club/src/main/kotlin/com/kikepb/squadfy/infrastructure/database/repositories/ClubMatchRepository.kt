package com.kikepb.squadfy.infrastructure.database.repositories

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMatchId
import com.kikepb.squadfy.infrastructure.database.entities.ClubMatchEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMatchEntity.MatchStatusEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface ClubMatchRepository : JpaRepository<ClubMatchEntity, ClubMatchId> {

    fun findAllByClubIdOrderByScheduledAtDesc(clubId: ClubId): List<ClubMatchEntity>
    fun existsByClubIdAndScheduledAtBetween(clubId: ClubId, start: Instant, end: Instant): Boolean
    fun findAllByStatusAndSignupClosesAtLessThanEqual(status: MatchStatusEntity, signupClosesAt: Instant): List<ClubMatchEntity>
}
