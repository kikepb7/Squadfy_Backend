package com.kikepb.squadfy.infrastructure.database.repositories

import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.MatchId
import com.kikepb.squadfy.infrastructure.database.entities.MatchEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface MatchRepository : JpaRepository<MatchEntity, MatchId> {

    fun findAllByClubIdOrderByScheduledAtDesc(clubId: ClubId): List<MatchEntity>

    fun findAllByClubIdAndStatusOrderByScheduledAtDesc(
        clubId: ClubId,
        status: MatchStatus
    ): List<MatchEntity>

    fun existsByClubIdAndStatusAndScheduledAtAfter(
        clubId: ClubId,
        status: MatchStatus,
        after: Instant
    ): Boolean

    fun existsByClubIdAndScheduledAtGreaterThanEqualAndScheduledAtLessThan(
        clubId: ClubId,
        from: Instant,
        to: Instant
    ): Boolean

    fun findFirstByClubIdAndStatusAndScheduledAtAfterOrderByScheduledAtAsc(
        clubId: ClubId,
        status: MatchStatus,
        after: Instant
    ): MatchEntity?

    fun findFirstByClubIdAndStatusOrderByScheduledAtDesc(clubId: ClubId, status: MatchStatus): MatchEntity?

    fun findFirstByClubIdAndStatusNotAndScheduledAtBeforeOrderByScheduledAtDesc(
        clubId: ClubId,
        status: MatchStatus,
        before: Instant
    ): MatchEntity?
}
