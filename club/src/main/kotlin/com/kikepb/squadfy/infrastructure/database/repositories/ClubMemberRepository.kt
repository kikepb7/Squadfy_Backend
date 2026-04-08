package com.kikepb.squadfy.infrastructure.database.repositories

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ClubMemberRepository : JpaRepository<ClubMemberEntity, ClubMemberId> {

    fun countByClubId(clubId: ClubId): Int
    fun existsByClubIdAndUserId(clubId: ClubId, userId: UserId): Boolean
    fun findByClubIdAndUserId(clubId: ClubId, userId: UserId): ClubMemberEntity?
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UserId): List<ClubMemberEntity>
    fun findAllByClubIdOrderByCreatedAtAsc(clubId: ClubId): List<ClubMemberEntity>

    @Query(
        """
        SELECT cm
        FROM ClubMemberEntity cm
        JOIN FETCH cm.userParticipant up
        WHERE cm.clubId = :clubId
        ORDER BY cm.createdAt ASC
        """
    )
    fun findAllByClubIdWithUserParticipant(clubId: ClubId): List<ClubMemberEntity>
}
