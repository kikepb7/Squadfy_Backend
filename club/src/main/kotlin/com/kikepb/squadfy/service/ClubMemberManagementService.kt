package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.events.club.ClubEvent
import com.kikepb.squadfy.domain.exception.ClubMemberNotFoundException
import com.kikepb.squadfy.domain.exception.ClubNotFoundException
import com.kikepb.squadfy.domain.exception.ClubOwnerCannotLeaveException
import com.kikepb.squadfy.domain.exception.ForbiddenException
import com.kikepb.squadfy.domain.exception.InvalidClubOperationException
import com.kikepb.squadfy.domain.model.ClubBanModel
import com.kikepb.squadfy.domain.model.ClubMemberModel
import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole
import com.kikepb.squadfy.domain.model.ClubModel
import com.kikepb.squadfy.domain.model.ClubRolePolicy
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity.ClubMemberRoleEntity.OWNER
import com.kikepb.squadfy.infrastructure.database.mappers.toClubMemberModel
import com.kikepb.squadfy.infrastructure.database.mappers.toClubMemberRole
import com.kikepb.squadfy.infrastructure.database.mappers.toClubModel
import com.kikepb.squadfy.infrastructure.database.mappers.toEntityRole
import com.kikepb.squadfy.infrastructure.database.repositories.ClubMemberRepository
import com.kikepb.squadfy.infrastructure.database.repositories.ClubRepository
import com.kikepb.squadfy.infrastructure.message_queue.EventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

/** Leaving, removing members, roles and ownership (spec 001 RN-8..RN-11). */
@Service
class ClubMemberManagementService(
    private val clubRepository: ClubRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val clubParticipantService: ClubParticipantService,
    private val clubMemberGuard: ClubMemberGuard,
    private val eventPublisher: EventPublisher,
    private val clock: Clock
) {

    @Transactional
    fun leaveClub(clubId: ClubId, userId: UserId) {
        val membership = clubMemberGuard.requireMember(clubId = clubId, userId = userId)
        if (membership.role == OWNER) throw ClubOwnerCannotLeaveException()

        deactivate(membership)
        eventPublisher.publishAfterCommit(
            ClubEvent.MemberLeft(
                clubId = clubId,
                clubName = clubName(clubId = clubId),
                clubMemberId = requireNotNull(membership.id),
                userId = userId
            )
        )
    }

    @Transactional
    fun removeMember(clubId: ClubId, userId: UserId, memberId: ClubMemberId) {
        val actor = clubMemberGuard.requireManager(clubId = clubId, userId = userId)
        val target = findActiveMember(clubId = clubId, memberId = memberId)
        if (target.id == actor.id) throw InvalidClubOperationException("Use leave to exit the club")
        if (!ClubRolePolicy.canRemove(actor = actor.role.toClubMemberRole(), target = target.role.toClubMemberRole())) {
            throw ForbiddenException()
        }

        deactivate(target)
        eventPublisher.publishAfterCommit(
            ClubEvent.MemberKicked(
                clubId = clubId,
                clubName = clubName(clubId = clubId),
                clubMemberId = memberId,
                kickedUserId = target.userId,
                kickedUsername = clubParticipantService.findById(userId = target.userId)?.username.orEmpty()
            )
        )
    }

    /**
     * Bans a current or former member (RN-14) with the same permissions as removing them; an
     * active member is also removed from the club.
     */
    @Transactional
    fun banMember(clubId: ClubId, userId: UserId, memberId: ClubMemberId) {
        val actor = clubMemberGuard.requireManager(clubId = clubId, userId = userId)
        val target = clubMemberRepository.findByIdAndClubId(id = memberId, clubId = clubId)
            ?: throw ClubMemberNotFoundException()
        if (target.id == actor.id) throw InvalidClubOperationException("You cannot ban yourself")
        if (!ClubRolePolicy.canRemove(actor = actor.role.toClubMemberRole(), target = target.role.toClubMemberRole())) {
            throw ForbiddenException()
        }
        if (target.bannedAt != null) return

        val wasActive = target.leftAt == null
        target.bannedAt = clock.instant()
        if (wasActive) target.leftAt = clock.instant()
        clubMemberRepository.saveAndFlush(target)

        if (wasActive) {
            eventPublisher.publishAfterCommit(
                ClubEvent.MemberKicked(
                    clubId = clubId,
                    clubName = clubName(clubId = clubId),
                    clubMemberId = memberId,
                    kickedUserId = target.userId,
                    kickedUsername = clubParticipantService.findById(userId = target.userId)?.username.orEmpty()
                )
            )
        }
    }

    /** Lifts a ban; the user can join again with a valid invitation code. */
    @Transactional
    fun unbanMember(clubId: ClubId, userId: UserId, memberId: ClubMemberId) {
        val actor = clubMemberGuard.requireManager(clubId = clubId, userId = userId)
        val target = clubMemberRepository.findByIdAndClubId(id = memberId, clubId = clubId)
            ?.takeIf { it.bannedAt != null }
            ?: throw ClubMemberNotFoundException()
        if (!ClubRolePolicy.canRemove(actor = actor.role.toClubMemberRole(), target = target.role.toClubMemberRole())) {
            throw ForbiddenException()
        }

        target.bannedAt = null
        clubMemberRepository.saveAndFlush(target)
    }

    fun getBans(clubId: ClubId, userId: UserId): List<ClubBanModel> {
        clubMemberGuard.requireManager(clubId = clubId, userId = userId)
        val banned = clubMemberRepository.findAllByClubIdAndBannedAtIsNotNullOrderByBannedAtDesc(clubId = clubId)
        if (banned.isEmpty()) return emptyList()
        val participants = clubParticipantService.findByIds(userIds = banned.map { it.userId })

        return banned.map {
            ClubBanModel(
                clubMemberId = requireNotNull(it.id),
                userId = it.userId,
                username = participants[it.userId]?.username.orEmpty(),
                bannedAt = requireNotNull(it.bannedAt)
            )
        }
    }

    @Transactional
    fun changeMemberRole(clubId: ClubId, userId: UserId, memberId: ClubMemberId, newRole: ClubMemberRole): ClubMemberModel {
        val actor = clubMemberGuard.requireManager(clubId = clubId, userId = userId)
        val target = findActiveMember(clubId = clubId, memberId = memberId)
        if (target.id == actor.id) throw InvalidClubOperationException("You cannot change your own role")
        if (newRole == ClubMemberRole.OWNER) throw InvalidClubOperationException("Use transfer-ownership to change the owner")
        if (!ClubRolePolicy.canAssign(actor = actor.role.toClubMemberRole(), target = target.role.toClubMemberRole(), newRole = newRole)) {
            throw ForbiddenException()
        }

        target.role = newRole.toEntityRole()
        clubMemberRepository.saveAndFlush(target)
        return target.toModel()
    }

    /** The chosen member becomes OWNER and the previous owner becomes ADMIN (RN-11). */
    @Transactional
    fun transferOwnership(clubId: ClubId, userId: UserId, newOwnerMemberId: ClubMemberId): ClubModel {
        val owner = clubMemberGuard.requireOwner(clubId = clubId, userId = userId)
        val newOwner = findActiveMember(clubId = clubId, memberId = newOwnerMemberId)
        if (newOwner.id == owner.id) throw InvalidClubOperationException("You already own this club")

        val club = clubRepository.findByIdOrNull(clubId) ?: throw ClubNotFoundException()
        owner.role = ClubMemberRole.ADMIN.toEntityRole()
        newOwner.role = ClubMemberRole.OWNER.toEntityRole()
        club.ownerId = newOwner.userId

        clubMemberRepository.saveAllAndFlush(listOf(owner, newOwner))
        clubRepository.saveAndFlush(club)
        return club.toClubModel(membersCount = clubMemberRepository.countByClubIdAndLeftAtIsNull(clubId = clubId))
    }

    private fun deactivate(membership: ClubMemberEntity) {
        membership.leftAt = clock.instant()
        clubMemberRepository.saveAndFlush(membership)
    }

    private fun findActiveMember(clubId: ClubId, memberId: ClubMemberId): ClubMemberEntity =
        clubMemberRepository.findByIdAndClubIdAndLeftAtIsNull(id = memberId, clubId = clubId)
            ?: throw ClubMemberNotFoundException()

    private fun clubName(clubId: ClubId): String =
        clubRepository.findByIdOrNull(clubId)?.name ?: throw ClubNotFoundException()

    private fun ClubMemberEntity.toModel(): ClubMemberModel {
        val participant = clubParticipantService.ensureExists(userId = userId)
        return toClubMemberModel(
            username = participant.username,
            email = participant.email,
            profilePictureUrl = participant.profilePictureUrl
        )
    }
}
