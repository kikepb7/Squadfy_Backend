package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.events.club.ClubEvent
import com.kikepb.squadfy.domain.model.OwnerSuccession
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.domain.user.UserDataEraser
import com.kikepb.squadfy.infrastructure.database.entities.ClubEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity.ClubMemberRoleEntity.OWNER
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity.ClubMemberRoleEntity.PLAYER
import com.kikepb.squadfy.infrastructure.database.mappers.toClubMemberRole
import com.kikepb.squadfy.infrastructure.database.repositories.ClubMemberRepository
import com.kikepb.squadfy.infrastructure.database.repositories.ClubParticipantRepository
import com.kikepb.squadfy.infrastructure.database.repositories.ClubRepository
import com.kikepb.squadfy.infrastructure.message_queue.EventPublisher
import com.kikepb.squadfy.infrastructure.storage.SupabaseStorageService
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

/**
 * Account deletion in the club module (spec 010 RN-A5, RN-A6): owned clubs pass to their successor
 * (or are deleted when nobody else is left), the user leaves every club and their participant is
 * anonymized. Memberships are kept, so past matches and stats of the other members still add up.
 */
@Service
@Order(1)
class ClubUserDataEraser(
    private val clubRepository: ClubRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val clubParticipantRepository: ClubParticipantRepository,
    private val storageService: SupabaseStorageService,
    private val eventPublisher: EventPublisher,
    private val clock: Clock
) : UserDataEraser {

    private val log = LoggerFactory.getLogger(ClubUserDataEraser::class.java)

    @Transactional
    override fun eraseUserData(userId: UserId) {
        clubMemberRepository.findAllByUserIdAndLeftAtIsNullOrderByCreatedAtDesc(userId = userId).forEach { membership ->
            val club = clubRepository.findByIdOrNull(membership.clubId) ?: return@forEach
            if (membership.role == OWNER && !handOver(club = club, owner = membership)) {
                deleteClub(club = club)
                return@forEach
            }
            leave(club = club, membership = membership)
        }

        clubParticipantRepository.findByIdOrNull(userId)?.let { participant ->
            participant.username = deletedUsername(userId)
            participant.email = "deleted+$userId@squadfy.invalid"
            participant.profilePictureUrl = null
            clubParticipantRepository.save(participant)
        }
    }

    /** @return false when the owner was the only active member. */
    private fun handOver(club: ClubEntity, owner: ClubMemberEntity): Boolean {
        val clubId = requireNotNull(club.id)
        val active = clubMemberRepository.findAllByClubIdAndLeftAtIsNullOrderByCreatedAtAsc(clubId = clubId)
        val successorId = OwnerSuccession.successor(
            owner = requireNotNull(owner.id),
            activeMembers = active.map {
                OwnerSuccession.Candidate(memberId = requireNotNull(it.id), role = it.role.toClubMemberRole(), joinedAt = it.createdAt)
            }
        ) ?: return false

        val successor = active.first { it.id == successorId }
        owner.role = PLAYER
        successor.role = OWNER
        club.ownerId = successor.userId
        clubMemberRepository.saveAllAndFlush(listOf(owner, successor))
        clubRepository.saveAndFlush(club)
        log.info("[AccountDeletion] Club={} passed to member={}", clubId, successorId)
        return true
    }

    private fun leave(club: ClubEntity, membership: ClubMemberEntity) {
        membership.leftAt = clock.instant()
        clubMemberRepository.saveAndFlush(membership)
        eventPublisher.publishAfterCommit(
            ClubEvent.MemberLeft(
                clubId = membership.clubId,
                clubName = club.name,
                clubMemberId = requireNotNull(membership.id),
                userId = membership.userId
            )
        )
    }

    private fun deleteClub(club: ClubEntity) {
        val clubId = requireNotNull(club.id)
        clubMemberRepository.deleteAllByClubId(clubId = clubId)
        clubMemberRepository.flush()
        clubRepository.delete(club)
        clubRepository.flush()
        club.clubLogoUrl?.let { logo ->
            try {
                storageService.deleteFile(url = logo)
            } catch (e: Exception) {
                log.warn("[AccountDeletion] Could not delete the logo of club={}", clubId, e)
            }
        }
        eventPublisher.publishAfterCommit(ClubEvent.ClubDeleted(clubId = clubId))
        log.info("[AccountDeletion] Club={} deleted: its owner was the last member", clubId)
    }

    companion object {
        /** Usernames are unique, so the anonymous name keeps a short random-looking suffix. */
        fun deletedUsername(userId: UserId) = "Usuario eliminado ${userId.toString().take(8)}"
    }
}
