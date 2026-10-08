package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.club.PlayerPosition
import com.kikepb.squadfy.domain.events.club.ClubEvent
import com.kikepb.squadfy.domain.exception.ClubCapacityReachedException
import com.kikepb.squadfy.domain.exception.ClubInviteCodeInvalidException
import com.kikepb.squadfy.domain.exception.ClubMemberBannedException
import com.kikepb.squadfy.domain.exception.ClubMembershipAlreadyExistsException
import com.kikepb.squadfy.domain.exception.ClubNotFoundException
import com.kikepb.squadfy.domain.exception.ClubParticipantNotFoundException
import com.kikepb.squadfy.domain.exception.InvalidClubOperationException
import com.kikepb.squadfy.domain.model.ClubMemberModel
import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole
import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole.PLAYER
import com.kikepb.squadfy.domain.model.ClubModel
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.domain.user.ProfilePictureProvider
import com.kikepb.squadfy.infrastructure.database.entities.ClubEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity.ClubMemberRoleEntity.ADMIN
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity.ClubMemberRoleEntity.OWNER
import com.kikepb.squadfy.infrastructure.database.mappers.toClubMemberModel
import com.kikepb.squadfy.infrastructure.database.mappers.toClubModel
import com.kikepb.squadfy.infrastructure.database.mappers.toEntityRole
import com.kikepb.squadfy.infrastructure.database.repositories.ClubMemberRepository
import com.kikepb.squadfy.infrastructure.database.repositories.ClubRepository
import com.kikepb.squadfy.infrastructure.message_queue.EventPublisher
import com.kikepb.squadfy.infrastructure.storage.SupabaseStorageService
import org.springframework.data.repository.findByIdOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom

@Service
class ClubService(
    private val clubRepository: ClubRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val clubParticipantService: ClubParticipantService,
    private val profilePictureProvider: ProfilePictureProvider,
    private val clubMemberGuard: ClubMemberGuard,
    private val storageService: SupabaseStorageService,
    private val eventPublisher: EventPublisher
) {

    private val logger = LoggerFactory.getLogger(ClubService::class.java)

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
        val memberships = clubMemberRepository.findAllByUserIdAndLeftAtIsNullOrderByCreatedAtDesc(userId = userId)
        val clubIds = memberships.map { it.clubId }.distinct()
        val clubs = clubRepository.findAllById(clubIds).associateBy { requireNotNull(it.id) }

        return clubIds.mapNotNull { clubId ->
            clubs[clubId]?.toClubModel(membersCount = activeMembersCount(clubId = clubId))
        }
    }

    fun getClubById(clubId: ClubId, userId: UserId): ClubModel {
        clubMemberGuard.requireMember(clubId = clubId, userId = userId)
        return findClub(clubId = clubId).toClubModel(membersCount = activeMembersCount(clubId = clubId))
    }

    /** Joins with an invitation code; a former member gets the same membership back as PLAYER (RN-12). */
    @Transactional
    fun joinClub(userId: UserId, invitationCode: String, shirtNumber: Int?, position: PlayerPosition?): ClubModel {
        val participant = ensureUserExists(userId = userId)

        val normalizedCode = invitationCode.trim().uppercase()
        val club = clubRepository.findByInvitationCode(invitationCode = normalizedCode) ?: throw ClubInviteCodeInvalidException()
        val clubId = requireNotNull(club.id)

        val previousMembership = clubMemberRepository.findByClubIdAndUserId(clubId = clubId, userId = userId)
        if (previousMembership?.bannedAt != null) throw ClubMemberBannedException()
        if (previousMembership != null && previousMembership.leftAt == null) throw ClubMembershipAlreadyExistsException()

        val membersCount = activeMembersCount(clubId = clubId)
        club.maxMembers?.let { if (membersCount >= it) throw ClubCapacityReachedException() }

        val membership = previousMembership?.apply {
            leftAt = null
            role = PLAYER.toEntityRole()
            shirtNumber?.let { this.shirtNumber = it }
            position?.let { this.position = it.name }
        } ?: ClubMemberEntity(
            clubId = clubId,
            userId = userId,
            shirtNumber = shirtNumber,
            position = position?.name,
            role = PLAYER.toEntityRole()
        )
        val saved = clubMemberRepository.saveAndFlush(membership)

        eventPublisher.publishAfterCommit(
            ClubEvent.MemberJoined(
                clubId = clubId,
                clubName = club.name,
                clubMemberId = requireNotNull(saved.id),
                newMemberUserId = userId,
                newMemberUsername = participant.username,
                adminUserIds = clubMemberRepository.findAllByClubIdAndLeftAtIsNullOrderByCreatedAtAsc(clubId = clubId)
                    .filter { it.role == OWNER || it.role == ADMIN }
                    .map { it.userId }
            )
        )

        return club.toClubModel(membersCount = membersCount + 1)
    }

    /** Edits the club's name, description and member limit (managers, RN-13). */
    @Transactional
    fun updateClub(clubId: ClubId, userId: UserId, name: String?, description: String?, maxMembers: Int?): ClubModel {
        clubMemberGuard.requireManager(clubId = clubId, userId = userId)
        val club = findClub(clubId = clubId)
        val membersCount = activeMembersCount(clubId = clubId)

        maxMembers?.let {
            if (it < membersCount) {
                throw InvalidClubOperationException("maxMembers cannot be lower than the current members ($membersCount)")
            }
            club.maxMembers = it
        }
        name?.let { club.name = it.trim() }
        description?.let { club.description = it.trim() }

        clubRepository.saveAndFlush(club)
        return club.toClubModel(membersCount = membersCount)
    }

    @Transactional
    fun updateClubLogo(clubId: ClubId, userId: UserId, bytes: ByteArray, mimeType: String): ClubModel {
        clubMemberGuard.requireManager(clubId = clubId, userId = userId)
        val club = findClub(clubId = clubId)
        club.clubLogoUrl = storageService.uploadImage(
            bucket = CLUB_LOGO_BUCKET,
            folder = CLUB_LOGO_FOLDER,
            bytes = bytes,
            mimeType = mimeType
        )
        clubRepository.saveAndFlush(club)
        return club.toClubModel(membersCount = activeMembersCount(clubId = clubId))
    }

    @Transactional
    fun regenerateInvitationCode(clubId: ClubId, userId: UserId): String {
        clubMemberGuard.requireManager(clubId = clubId, userId = userId)
        val club = findClub(clubId = clubId)
        val newCode = generateUniqueInvitationCode()
        club.invitationCode = newCode
        clubRepository.saveAndFlush(club)
        return newCode
    }

    fun getMembers(clubId: ClubId, userId: UserId): List<ClubMemberModel> {
        clubMemberGuard.requireMember(clubId = clubId, userId = userId)
        val members = clubMemberRepository.findAllActiveByClubIdWithUserParticipant(clubId = clubId)
        val profilePictures = profilePictureProvider.findProfilePictures(userIds = members.map { it.userId })

        return members.map { member ->
            val userSnapshot = member.userParticipant ?: throw ClubParticipantNotFoundException(userId = member.userId)
            member.toClubMemberModel(
                username = userSnapshot.username,
                email = userSnapshot.email,
                profilePictureUrl = profilePictures[member.userId]
            )
        }
    }

    @Transactional
    fun updateMyMembership(clubId: ClubId, userId: UserId, shirtNumber: Int?, position: PlayerPosition?): ClubMemberModel {
        val membership = clubMemberGuard.requireMember(clubId = clubId, userId = userId)

        shirtNumber?.let { membership.shirtNumber = it }
        position?.let { membership.position = it.name }
        clubMemberRepository.saveAndFlush(membership)
        return membership.toModelWithProfile()
    }

    /** The member's own picture in this club (spec 012 RN-C1); the previous one is removed from storage. */
    @Transactional
    fun updateMyClubPicture(clubId: ClubId, userId: UserId, bytes: ByteArray, mimeType: String): ClubMemberModel {
        val membership = clubMemberGuard.requireMember(clubId = clubId, userId = userId)
        if (mimeType !in SupabaseStorageService.ALLOWED_IMAGE_MIME_TYPES) {
            throw InvalidClubOperationException("Unsupported image type: $mimeType. Allowed: ${SupabaseStorageService.ALLOWED_IMAGE_MIME_TYPES.keys}")
        }
        val previous = membership.clubPictureUrl
        membership.clubPictureUrl = storageService.uploadImage(
            bucket = CLUB_LOGO_BUCKET,
            folder = MEMBER_PICTURE_FOLDER,
            bytes = bytes,
            mimeType = mimeType
        )
        clubMemberRepository.saveAndFlush(membership)
        previous?.let { deleteStoredPicture(url = it) }
        return membership.toModelWithProfile()
    }

    @Transactional
    fun deleteMyClubPicture(clubId: ClubId, userId: UserId): ClubMemberModel {
        val membership = clubMemberGuard.requireMember(clubId = clubId, userId = userId)
        membership.clubPictureUrl?.let { url ->
            membership.clubPictureUrl = null
            clubMemberRepository.saveAndFlush(membership)
            deleteStoredPicture(url = url)
        }
        return membership.toModelWithProfile()
    }

    private fun deleteStoredPicture(url: String) {
        try {
            storageService.deleteFile(url = url)
        } catch (e: Exception) {
            logger.warn("Could not delete the club picture {}: {}", url, e.message)
        }
    }

    private fun ClubMemberEntity.toModelWithProfile(): ClubMemberModel {
        val participant = clubParticipantService.ensureExists(userId = userId)
        return toClubMemberModel(
            username = participant.username,
            email = participant.email,
            profilePictureUrl = profilePictureProvider.findProfilePictures(userIds = listOf(userId))[userId]
        )
    }

    private fun findClub(clubId: ClubId): ClubEntity =
        clubRepository.findByIdOrNull(clubId) ?: throw ClubNotFoundException()

    private fun activeMembersCount(clubId: ClubId): Int =
        clubMemberRepository.countByClubIdAndLeftAtIsNull(clubId = clubId)

    private fun ensureUserExists(userId: UserId) = clubParticipantService.ensureExists(userId = userId)

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
        const val MEMBER_PICTURE_FOLDER = "club-members"
    }
}
