package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.exception.ForbiddenException
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity.ClubMemberRoleEntity.ADMIN
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity.ClubMemberRoleEntity.OWNER
import com.kikepb.squadfy.infrastructure.database.repositories.ClubMemberRepository
import org.springframework.stereotype.Component

/** Permission checks on active memberships, shared by the club use cases. */
@Component
class ClubMemberGuard(
    private val clubMemberRepository: ClubMemberRepository
) {

    fun requireMember(clubId: ClubId, userId: UserId): ClubMemberEntity =
        clubMemberRepository.findByClubIdAndUserIdAndLeftAtIsNull(clubId = clubId, userId = userId)
            ?: throw ForbiddenException()

    fun requireManager(clubId: ClubId, userId: UserId): ClubMemberEntity =
        requireMember(clubId = clubId, userId = userId).also {
            if (it.role != OWNER && it.role != ADMIN) throw ForbiddenException()
        }

    fun requireOwner(clubId: ClubId, userId: UserId): ClubMemberEntity =
        requireMember(clubId = clubId, userId = userId).also {
            if (it.role != OWNER) throw ForbiddenException()
        }
}
