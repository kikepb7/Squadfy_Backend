package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.exception.ClubCapacityReachedException
import com.kikepb.squadfy.domain.exception.ClubInviteCodeInvalidException
import com.kikepb.squadfy.domain.exception.ClubMatchNotFoundException
import com.kikepb.squadfy.domain.exception.ClubMembershipAlreadyExistsException
import com.kikepb.squadfy.domain.exception.ClubNotFoundException
import com.kikepb.squadfy.domain.exception.ClubParticipantNotFoundException
import com.kikepb.squadfy.domain.exception.ForbiddenException
import com.kikepb.squadfy.domain.exception.InvalidTeamGenerationRequestException
import com.kikepb.squadfy.domain.model.ClubMatchModel
import com.kikepb.squadfy.domain.model.ClubMemberModel
import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole
import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole.PLAYER
import com.kikepb.squadfy.domain.model.ClubModel
import com.kikepb.squadfy.domain.model.TeamSideModel.TEAM_A
import com.kikepb.squadfy.domain.model.TeamSideModel.TEAM_B
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMatchId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.ClubEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMatchEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity.ClubMemberRoleEntity.*
import com.kikepb.squadfy.infrastructure.database.entities.MatchTeamPlayerEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toClubMatchModel
import com.kikepb.squadfy.infrastructure.database.mappers.toClubMemberModel
import com.kikepb.squadfy.infrastructure.database.mappers.toClubModel
import com.kikepb.squadfy.infrastructure.database.mappers.toEntityRole
import com.kikepb.squadfy.infrastructure.database.mappers.toEntitySide
import com.kikepb.squadfy.infrastructure.database.repositories.ClubMatchRepository
import com.kikepb.squadfy.infrastructure.database.repositories.ClubMemberRepository
import com.kikepb.squadfy.infrastructure.database.repositories.ClubRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MatchTeamPlayerRepository
import com.kikepb.squadfy.service.ClubService.TeamGenerationMode.AUTO
import com.kikepb.squadfy.service.ClubService.TeamGenerationMode.MANUAL
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant
import kotlin.random.Random

@Service
class ClubService(
    private val clubRepository: ClubRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val clubMatchRepository: ClubMatchRepository,
    private val matchTeamPlayerRepository: MatchTeamPlayerRepository,
    private val clubParticipantService: ClubParticipantService
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
    fun joinClub(userId: UserId, invitationCode: String, shirtNumber: Int?, position: String?): ClubModel {
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
                position = position?.trim(),
                role = PLAYER.toEntityRole()
            )
        )

        return club.toClubModel(membersCount = membersCount + 1)
    }

    @Transactional
    fun updateClubLogo(clubId: ClubId, userId: UserId, logoUrl: String): ClubModel {
        ensureCanManageClub(clubId = clubId, userId = userId)
        val club = clubRepository.findByIdOrNull(clubId) ?: throw ClubNotFoundException()
        club.clubLogoUrl = logoUrl
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
            member.toClubMemberModel(username = userSnapshot.username, email = userSnapshot.email)
        }
    }

    @Transactional
    fun createMatch(clubId: ClubId, userId: UserId, scheduledAt: Instant?): ClubMatchModel {
        ensureCanManageClub(clubId = clubId, userId = userId)
        if (!clubRepository.existsById(clubId)) throw ClubNotFoundException()

        val match = clubMatchRepository.saveAndFlush(
            ClubMatchEntity(
                clubId = clubId,
                createdByUserId = userId,
                scheduledAt = scheduledAt ?: Instant.now()
            )
        )

        return match.toClubMatchModel(assignments = emptyList())
    }

    @Transactional
    fun generateTeams(
        matchId: ClubMatchId,
        userId: UserId,
        mode: TeamGenerationMode,
        manualTeamA: List<ClubMemberId>?,
        manualTeamB: List<ClubMemberId>?
    ): ClubMatchModel {
        val match = clubMatchRepository.findByIdOrNull(id = matchId) ?: throw ClubMatchNotFoundException()
        ensureCanManageClub(clubId = match.clubId, userId = userId)

        val clubMembers = clubMemberRepository.findAllByClubIdOrderByCreatedAtAsc(clubId = match.clubId)
        if (clubMembers.size < 2) throw InvalidTeamGenerationRequestException(message = "At least 2 members are required.")

        val assignment = when (mode) {
            AUTO -> autoAssignTeams(clubMembers.mapNotNull { it.id })
            MANUAL -> manualAssignTeams(
                clubMembers = clubMembers.mapNotNull { it.id }.toSet(),
                teamA = manualTeamA,
                teamB = manualTeamB
            )
        }

        matchTeamPlayerRepository.deleteByMatchId(matchId = matchId)
        matchTeamPlayerRepository.saveAll(
            assignment.first.map {
                MatchTeamPlayerEntity(
                    matchId = matchId,
                    clubMemberId = it,
                    teamSide = TEAM_A.toEntitySide()
                )
            } + assignment.second.map {
                MatchTeamPlayerEntity(
                    matchId = matchId,
                    clubMemberId = it,
                    teamSide = TEAM_B.toEntitySide()
                )
            }
        )

        val savedAssignments = matchTeamPlayerRepository.findAllByMatchId(matchId = matchId)
        return match.toClubMatchModel(assignments = savedAssignments)
    }

    fun getMatch(matchId: ClubMatchId, userId: UserId): ClubMatchModel {
        val match = clubMatchRepository.findByIdOrNull(matchId) ?: throw ClubMatchNotFoundException()
        ensureIsClubMember(clubId = match.clubId, userId = userId)
        val assignments = matchTeamPlayerRepository.findAllByMatchId(matchId = matchId)
        return match.toClubMatchModel(assignments = assignments)
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

    private fun autoAssignTeams(memberIds: List<ClubMemberId>): Pair<List<ClubMemberId>, List<ClubMemberId>> {
        val shuffled = memberIds.shuffled(Random(Instant.now().toEpochMilli()))
        val splitIndex = shuffled.size / 2
        val teamA = shuffled.take(splitIndex + (shuffled.size % 2))
        val teamB = shuffled.drop(teamA.size)
        return teamA to teamB
    }

    private fun manualAssignTeams(
        clubMembers: Set<ClubMemberId>,
        teamA: List<ClubMemberId>?,
        teamB: List<ClubMemberId>?
    ): Pair<List<ClubMemberId>, List<ClubMemberId>> {
        val safeTeamA = teamA?.distinct() ?: emptyList()
        val safeTeamB = teamB?.distinct() ?: emptyList()
        if (safeTeamA.isEmpty() || safeTeamB.isEmpty()) {
            throw InvalidTeamGenerationRequestException(message = "Manual mode requires non-empty teamA and teamB.")
        }

        val allManualMembers = safeTeamA + safeTeamB
        if (allManualMembers.size != allManualMembers.toSet().size) {
            throw InvalidTeamGenerationRequestException(message = "A player cannot be in both teams.")
        }
        if (!clubMembers.containsAll(allManualMembers)) {
            throw InvalidTeamGenerationRequestException(message = "All players must belong to the club.")
        }
        if (kotlin.math.abs(safeTeamA.size - safeTeamB.size) > 1) {
            throw InvalidTeamGenerationRequestException(message = "Team sizes must be balanced (difference <= 1).")
        }

        return safeTeamA to safeTeamB
    }

    enum class TeamGenerationMode {
        AUTO,
        MANUAL
    }
}
