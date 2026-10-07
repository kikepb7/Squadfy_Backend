package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.club.ClubMembershipProvider
import com.kikepb.squadfy.domain.club.ClubMembershipSnapshot
import com.kikepb.squadfy.domain.club.ClubRole
import com.kikepb.squadfy.domain.club.PlayerPosition
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity.ClubMemberRoleEntity
import com.kikepb.squadfy.infrastructure.database.repositories.ClubMemberRepository
import com.kikepb.squadfy.infrastructure.database.repositories.ClubRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ClubMembershipQueryService(
    private val clubMemberRepository: ClubMemberRepository,
    private val clubRepository: ClubRepository
) : ClubMembershipProvider {

    override fun findMembership(clubId: ClubId, userId: UserId): ClubMembershipSnapshot? =
        clubMemberRepository.findByClubIdAndUserIdAndLeftAtIsNull(clubId = clubId, userId = userId)?.toSnapshot()

    override fun findMembers(clubId: ClubId, memberIds: Collection<ClubMemberId>): List<ClubMembershipSnapshot> {
        if (memberIds.isEmpty()) return emptyList()
        return clubMemberRepository.findAllByClubIdAndIdInAndLeftAtIsNull(clubId = clubId, ids = memberIds).map { it.toSnapshot() }
    }

    override fun findAllMembers(clubId: ClubId): List<ClubMembershipSnapshot> =
        clubMemberRepository.findAllByClubIdAndLeftAtIsNullOrderByCreatedAtAsc(clubId = clubId).map { it.toSnapshot() }

    override fun findClubName(clubId: ClubId): String? =
        clubRepository.findByIdOrNull(clubId)?.name

    override fun findMemberIdsOfUser(userId: UserId): List<ClubMemberId> =
        clubMemberRepository.findAllByUserId(userId = userId).map { requireNotNull(it.id) }

    private fun ClubMemberEntity.toSnapshot(): ClubMembershipSnapshot =
        ClubMembershipSnapshot(
            memberId = requireNotNull(id),
            clubId = clubId,
            userId = userId,
            role = role.toClubRole(),
            position = PlayerPosition.fromRaw(position)
        )

    private fun ClubMemberRoleEntity.toClubRole(): ClubRole =
        when (this) {
            ClubMemberRoleEntity.OWNER   -> ClubRole.OWNER
            ClubMemberRoleEntity.ADMIN   -> ClubRole.ADMIN
            ClubMemberRoleEntity.CAPTAIN -> ClubRole.CAPTAIN
            ClubMemberRoleEntity.PLAYER  -> ClubRole.PLAYER
        }
}
