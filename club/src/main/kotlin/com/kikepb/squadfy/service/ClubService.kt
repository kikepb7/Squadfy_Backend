package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.exception.ClubCapacityReachedException
import com.kikepb.squadfy.domain.exception.ClubInviteCodeInvalidException
import com.kikepb.squadfy.domain.exception.ClubMembershipAlreadyExistsException
import com.kikepb.squadfy.domain.exception.ClubNotFoundException
import com.kikepb.squadfy.domain.exception.ClubParticipantNotFoundException
import com.kikepb.squadfy.domain.club.PlayerPosition
import com.kikepb.squadfy.domain.exception.ForbiddenException
import com.kikepb.squadfy.domain.model.ClubMemberModel
import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole
import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole.PLAYER
import com.kikepb.squadfy.domain.model.ClubModel
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.ClubEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity.ClubMemberRoleEntity.ADMIN
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity.ClubMemberRoleEntity.OWNER
import com.kikepb.squadfy.infrastructure.database.mappers.toClubMemberModel
import com.kikepb.squadfy.infrastructure.database.mappers.toClubModel
import com.kikepb.squadfy.infrastructure.database.mappers.toEntityRole
import com.kikepb.squadfy.infrastructure.database.repositories.ClubMemberRepository
import com.kikepb.squadfy.infrastructure.database.repositories.ClubRepository
import com.kikepb.squadfy.infrastructure.storage.SupabaseStorageService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom

@Service
class ClubService(
    private val clubRepository: ClubRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val clubParticipantService: ClubParticipantService,
    private val storageService: SupabaseStorageService
) {
    @Transactional
    fun createClub(userId: UserId, name: String, description: String?, clubLogoUrl: String?, maxMembers: Int?): ClubModel {
        ensureUserExists(userId = userId)

        val savedClub = clubRepository.saveAndFlush(
            ClubEntity(
                name = name.trim(),
                description = description?.trim(),
                clubLogoUrl = clubLogoUrl?.trim(),
                ownerId = userId,
                invitationCode = generateUniqueInvitationCode(),
                maxMembers = maxMembers
            )
        )

        clubMemberRepository.saveAndFlush(
            ClubMemberEntity(
                clubId = requireNotNull(savedClub.id),
                userId = userId,
                role = ClubMemberRole.OWNER.toEntityRole()
            )
        )

        return savedClub.toClubModel(membersCount = 1)
    }

    fun getClubsForUser(userId: UserId): List<ClubModel> {
        val memberships = clubMemberRepository.findAllByUserIdOrderByCreatedAtDesc(userId = userId)
        val clubIds = memberships.map { it.clubId }.distinct()
        val clubs = clubRepository.findAllById(clubIds).associateBy { requireNotNull(it.id) }

        return clubIds.mapNotNull { clubId ->
            clubs[clubId]?.toClubModel(membersCount = clubMemberRepository.countByClubId(clubId = clubId))
        }
    }

    fun getClubById(clubId: ClubId, userId: UserId): ClubModel {
        ensureIsClubMember(clubId = clubId, userId = userId)
        val club = clubRepository.findByIdOrNull(clubId) ?: throw ClubNotFoundException()
        return club.toClubModel(membersCount = clubMemberRepository.countByClubId(clubId = clubId))
    }

    @Transactional
    fun joinClub(userId: UserId, invitationCode: String, shirtNumber: Int?, position: PlayerPosition?): ClubModel {
        ensureUserExists(userId = userId)

        val normalizedCode = invitationCode.trim().uppercase()
        val club = clubRepository.findByInvitationCode(invitationCode = normalizedCode) ?: throw ClubInviteCodeInvalidException()
        val clubId = requireNotNull(club.id)

        if (clubMemberRepository.existsByClubIdAndUserId(clubId = clubId, userId = userId)) throw ClubMembershipAlreadyExistsException()

        val membersCount = clubMemberRepository.countByClubId(clubId = clubId)
        if (club.maxMembers != null && membersCount >= club.maxMembers!!) throw ClubCapacityReachedException()

        clubMemberRepository.saveAndFlush(
            ClubMemberEntity(
                clubId = clubId,
                userId = userId,
                shirtNumber = shirtNumber,
                position = position?.name,
                role = PLAYER.toEntityRole()
            )
        )

        return club.toClubModel(membersCount = membersCount + 1)
    }

    @Transactional
    fun updateClubLogo(clubId: ClubId, userId: UserId, bytes: ByteArray, mimeType: String): ClubModel {
        ensureCanManageClub(clubId = clubId, userId = userId)
        val club = clubRepository.findByIdOrNull(clubId) ?: throw ClubNotFoundException()
        club.clubLogoUrl = storageService.uploadImage(
            bucket = CLUB_LOGO_BUCKET,
            folder = CLUB_LOGO_FOLDER,
            bytes = bytes,
            mimeType = mimeType
        )
        clubRepository.saveAndFlush(club)
        return club.toClubModel(membersCount = clubMemberRepository.countByClubId(clubId = clubId))
    }

    @Transactional
    fun regenerateInvitationCode(clubId: ClubId, userId: UserId): String {
        ensureCanManageClub(clubId = clubId, userId = userId)
        val club = clubRepository.findByIdOrNull(clubId) ?: throw ClubNotFoundException()
        val newCode = generateUniqueInvitationCode()
        club.invitationCode = newCode
        clubRepository.saveAndFlush(club)
        return newCode
    }

    fun getMembers(clubId: ClubId, userId: UserId): List<ClubMemberModel> {
        ensureIsClubMember(clubId = clubId, userId = userId)
        val members = clubMemberRepository.findAllByClubIdWithUserParticipant(clubId = clubId)

        return members.map { member ->
            val userSnapshot = member.userParticipant ?: throw ClubParticipantNotFoundException(userId = member.userId)
            member.toClubMemberModel(
                username = userSnapshot.username,
                email = userSnapshot.email,
                profilePictureUrl = userSnapshot.profilePictureUrl
            )
        }
    }

    @Transactional
    fun updateMyMembership(clubId: ClubId, userId: UserId, shirtNumber: Int?, position: PlayerPosition?): ClubMemberModel {
        val membership = clubMemberRepository.findByClubIdAndUserId(clubId = clubId, userId = userId)
            ?: throw ForbiddenException()

        shirtNumber?.let { membership.shirtNumber = it }
        position?.let { membership.position = it.name }
        clubMemberRepository.saveAndFlush(membership)

        val participant = clubParticipantService.ensureExists(userId = userId)
        return membership.toClubMemberModel(
            username = participant.username,
            email = participant.email,
            profilePictureUrl = participant.profilePictureUrl
        )
    }

    private fun ensureCanManageClub(clubId: ClubId, userId: UserId) {
        val membership = clubMemberRepository.findByClubIdAndUserId(clubId = clubId, userId = userId)
            ?: throw ForbiddenException()

        if (membership.role !in setOf(OWNER, ADMIN)) throw ForbiddenException()
    }

    private fun ensureIsClubMember(clubId: ClubId, userId: UserId) {
        if (!clubMemberRepository.existsByClubIdAndUserId(clubId = clubId, userId = userId)) throw ForbiddenException()
    }

    private fun ensureUserExists(userId: UserId) {
        clubParticipantService.ensureExists(userId = userId)
    }

    private fun generateUniqueInvitationCode(): String {
        var code = randomCode()
        while (clubRepository.existsByInvitationCode(invitationCode = code)) {
            code = randomCode()
        }
        return code
    }

    private fun randomCode(length: Int = 8): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val secureRandom = SecureRandom()
        return buildString {
            repeat(length) {
                append(chars[secureRandom.nextInt(chars.length)])
            }
        }
    }

    private companion object {
        const val CLUB_LOGO_BUCKET = "profile-pictures"
        const val CLUB_LOGO_FOLDER = "clubs"
    }
}
