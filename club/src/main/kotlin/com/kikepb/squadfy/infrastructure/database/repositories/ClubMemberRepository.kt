package com.kikepb.squadfy.infrastructure.database.repositories

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/** Memberships are soft-deleted: "active" means left_at IS NULL (spec 001 RN-10). */
interface ClubMemberRepository : JpaRepository<ClubMemberEntity, ClubMemberId> {

    fun countByClubIdAndLeftAtIsNull(clubId: ClubId): Int

    fun findByClubIdAndUserIdAndLeftAtIsNull(clubId: ClubId, userId: UserId): ClubMemberEntity?

    fun findByIdAndClubIdAndLeftAtIsNull(id: ClubMemberId, clubId: ClubId): ClubMemberEntity?

    fun findAllByUserIdAndLeftAtIsNullOrderByCreatedAtDesc(userId: UserId): List<ClubMemberEntity>

    fun findAllByClubIdAndLeftAtIsNullOrderByCreatedAtAsc(clubId: ClubId): List<ClubMemberEntity>

    fun findAllByClubIdAndIdInAndLeftAtIsNull(clubId: ClubId, ids: Collection<ClubMemberId>): List<ClubMemberEntity>

    /** Any membership of the club, active or not (bans also apply to former members). */
    fun findByIdAndClubId(id: ClubMemberId, clubId: ClubId): ClubMemberEntity?

    fun findAllByClubIdAndBannedAtIsNotNullOrderByBannedAtDesc(clubId: ClubId): List<ClubMemberEntity>

    /** Any membership, active or not: used to reactivate a former member (spec 001 RN-12). */
    fun findByClubIdAndUserId(clubId: ClubId, userId: UserId): ClubMemberEntity?

    @Query(
        """
        SELECT cm
        FROM ClubMemberEntity cm
        JOIN FETCH cm.userParticipant up
        WHERE cm.clubId = :clubId AND cm.leftAt IS NULL
        ORDER BY cm.createdAt ASC
        """
    )
    fun findAllActiveByClubIdWithUserParticipant(clubId: ClubId): List<ClubMemberEntity>
}
