package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.exception.AlreadySignedUpException
import com.kikepb.squadfy.domain.exception.ClubCapacityReachedException
import com.kikepb.squadfy.domain.exception.ClubInviteCodeInvalidException
import com.kikepb.squadfy.domain.exception.ClubMatchNotFoundException
import com.kikepb.squadfy.domain.exception.ClubMembershipAlreadyExistsException
import com.kikepb.squadfy.domain.exception.ClubNotFoundException
import com.kikepb.squadfy.domain.exception.ClubParticipantNotFoundException
import com.kikepb.squadfy.domain.exception.ClubScheduleExceptionNotFoundException
import com.kikepb.squadfy.domain.exception.ForbiddenException
import com.kikepb.squadfy.domain.exception.InvalidScheduleException
import com.kikepb.squadfy.domain.exception.InvalidTeamGenerationRequestException
import com.kikepb.squadfy.domain.exception.MatchAlreadyCompletedException
import com.kikepb.squadfy.domain.exception.MatchSignupNotFoundException
import com.kikepb.squadfy.domain.exception.SignupWindowClosedException
import com.kikepb.squadfy.domain.model.ClubMatchModel
import com.kikepb.squadfy.domain.model.ClubMemberModel
import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole
import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole.PLAYER
import com.kikepb.squadfy.domain.model.ClubModel
import com.kikepb.squadfy.domain.model.ClubScheduleExceptionModel
import com.kikepb.squadfy.domain.model.MatchParticipantModel
import com.kikepb.squadfy.domain.model.MatchSignupModel
import com.kikepb.squadfy.domain.model.PlayerPositionModel
import com.kikepb.squadfy.domain.model.TeamSideModel.TEAM_A
import com.kikepb.squadfy.domain.model.TeamSideModel.TEAM_B
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMatchId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.ClubScheduleExceptionId
import com.kikepb.squadfy.domain.type.MatchSignupId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.ClubEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMatchEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMatchEntity.MatchStatusEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity.ClubMemberRoleEntity.*
import com.kikepb.squadfy.infrastructure.database.entities.ClubScheduleExceptionEntity
import com.kikepb.squadfy.infrastructure.database.entities.MatchSignupEntity
import com.kikepb.squadfy.infrastructure.database.entities.MatchSignupEntity.SignupStatusEntity
import com.kikepb.squadfy.infrastructure.database.entities.MatchTeamPlayerEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toClubMatchModel
import com.kikepb.squadfy.infrastructure.database.mappers.toClubMemberModel
import com.kikepb.squadfy.infrastructure.database.mappers.toClubModel
import com.kikepb.squadfy.infrastructure.database.mappers.toClubScheduleExceptionModel
import com.kikepb.squadfy.infrastructure.database.mappers.toEntityPosition
import com.kikepb.squadfy.infrastructure.database.mappers.toEntityRole
import com.kikepb.squadfy.infrastructure.database.mappers.toEntitySide
import com.kikepb.squadfy.infrastructure.database.mappers.toMatchParticipantModel
import com.kikepb.squadfy.infrastructure.database.mappers.toMatchSignupModel
import com.kikepb.squadfy.infrastructure.database.mappers.toPlayerPositionModel
import com.kikepb.squadfy.infrastructure.database.repositories.ClubMatchRepository
import com.kikepb.squadfy.infrastructure.database.repositories.ClubMemberRepository
import com.kikepb.squadfy.infrastructure.database.repositories.ClubRepository
import com.kikepb.squadfy.infrastructure.database.repositories.ClubScheduleExceptionRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MatchSignupRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MatchTeamPlayerRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Service
class ClubService(
    private val clubRepository: ClubRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val clubMatchRepository: ClubMatchRepository,
    private val matchTeamPlayerRepository: MatchTeamPlayerRepository,
    private val matchSignupRepository: MatchSignupRepository,
    private val clubScheduleExceptionRepository: ClubScheduleExceptionRepository,
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
    fun joinClub(userId: UserId, invitationCode: String, shirtNumber: Int?, position: PlayerPositionModel?): ClubModel {
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
                position = position?.toEntityPosition(),
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
            member.toClubMemberModel(username = userSnapshot.username, email = userSnapshot.email, profilePictureUrl = userSnapshot.profilePictureUrl)
        }
    }

    @Transactional
    fun updateMember(clubId: ClubId, userId: UserId, memberId: ClubMemberId, shirtNumber: Int?, position: PlayerPositionModel?, rating: Int?): ClubMemberModel {
        ensureCanManageClub(clubId = clubId, userId = userId)
        val member = clubMemberRepository.findByIdOrNull(memberId)?.takeIf { it.clubId == clubId }
            ?: throw ClubParticipantNotFoundException(userId = userId)

        shirtNumber?.let { member.shirtNumber = it }
        position?.let { member.position = it.toEntityPosition() }
        rating?.let { member.rating = it.coerceIn(1, 99) }
        clubMemberRepository.saveAndFlush(member)

        val userSnapshot = member.userParticipant ?: throw ClubParticipantNotFoundException(userId = member.userId)
        return member.toClubMemberModel(username = userSnapshot.username, email = userSnapshot.email, profilePictureUrl = userSnapshot.profilePictureUrl)
    }

    @Transactional
    fun updateSchedule(
        clubId: ClubId,
        userId: UserId,
        matchDayOfWeek: DayOfWeek?,
        matchStartTime: LocalTime?,
        matchEndTime: LocalTime?,
        seasonStartMonth: Int?,
        seasonStartDay: Int?,
        drawTime: LocalTime?
    ): ClubModel {
        ensureCanManageClub(clubId = clubId, userId = userId)
        val club = clubRepository.findByIdOrNull(clubId) ?: throw ClubNotFoundException()

        if (matchStartTime != null && matchEndTime != null && !matchStartTime.isBefore(matchEndTime)) {
            throw InvalidScheduleException(message = "Match start time must be before end time.")
        }

        val month = seasonStartMonth ?: club.seasonStartMonth
        val day = seasonStartDay ?: club.seasonStartDay
        try {
            java.time.MonthDay.of(month, day)
        } catch (e: java.time.DateTimeException) {
            throw InvalidScheduleException(message = "Invalid season start date.")
        }

        club.matchDayOfWeek = matchDayOfWeek
        club.matchStartTime = matchStartTime
        club.matchEndTime = matchEndTime
        club.seasonStartMonth = month
        club.seasonStartDay = day
        drawTime?.let { club.drawTime = it }

        clubRepository.saveAndFlush(club)
        return club.toClubModel(membersCount = clubMemberRepository.countByClubId(clubId = clubId))
    }

    fun listScheduleExceptions(clubId: ClubId, userId: UserId): List<ClubScheduleExceptionModel> {
        ensureIsClubMember(clubId = clubId, userId = userId)
        return clubScheduleExceptionRepository.findAllByClubIdOrderByDateAsc(clubId = clubId).map { it.toClubScheduleExceptionModel() }
    }

    @Transactional
    fun addScheduleException(clubId: ClubId, userId: UserId, date: LocalDate, reason: String?): ClubScheduleExceptionModel {
        ensureCanManageClub(clubId = clubId, userId = userId)
        if (!clubRepository.existsById(clubId)) throw ClubNotFoundException()

        val saved = clubScheduleExceptionRepository.saveAndFlush(
            ClubScheduleExceptionEntity(clubId = clubId, date = date, reason = reason?.trim())
        )
        return saved.toClubScheduleExceptionModel()
    }

    @Transactional
    fun removeScheduleException(clubId: ClubId, userId: UserId, exceptionId: ClubScheduleExceptionId) {
        ensureCanManageClub(clubId = clubId, userId = userId)
        val exception = clubScheduleExceptionRepository.findByIdAndClubId(id = exceptionId, clubId = clubId)
            ?: throw ClubScheduleExceptionNotFoundException()
        clubScheduleExceptionRepository.delete(exception)
    }

    @Transactional
    fun createMatch(clubId: ClubId, userId: UserId, scheduledAt: Instant?, signupOpensAt: Instant?, signupClosesAt: Instant?): ClubMatchModel {
        ensureCanManageClub(clubId = clubId, userId = userId)
        if (!clubRepository.existsById(clubId)) throw ClubNotFoundException()

        val effectiveScheduledAt = scheduledAt ?: Instant.now()
        val match = clubMatchRepository.saveAndFlush(
            ClubMatchEntity(
                clubId = clubId,
                createdByUserId = userId,
                scheduledAt = effectiveScheduledAt,
                signupOpensAt = signupOpensAt ?: Instant.now(),
                signupClosesAt = signupClosesAt ?: effectiveScheduledAt
            )
        )

        return match.toClubMatchModel(teamA = emptyList(), teamB = emptyList())
    }

    @Transactional
    fun cancelMatch(matchId: ClubMatchId, userId: UserId): ClubMatchModel {
        val match = clubMatchRepository.findByIdOrNull(matchId) ?: throw ClubMatchNotFoundException()
        ensureCanManageClub(clubId = match.clubId, userId = userId)
        match.status = MatchStatusEntity.CANCELLED
        clubMatchRepository.saveAndFlush(match)
        return buildMatchModel(match)
    }

    fun getMatchesForClub(clubId: ClubId, userId: UserId): List<ClubMatchModel> {
        ensureIsClubMember(clubId = clubId, userId = userId)
        return clubMatchRepository.findAllByClubIdOrderByScheduledAtDesc(clubId = clubId).map { buildMatchModel(it) }
    }

    fun getMatch(matchId: ClubMatchId, userId: UserId): ClubMatchModel {
        val match = clubMatchRepository.findByIdOrNull(matchId) ?: throw ClubMatchNotFoundException()
        ensureIsClubMember(clubId = match.clubId, userId = userId)
        return buildMatchModel(match)
    }

    fun listSignups(matchId: ClubMatchId, userId: UserId): List<MatchSignupModel> {
        val match = clubMatchRepository.findByIdOrNull(matchId) ?: throw ClubMatchNotFoundException()
        ensureIsClubMember(clubId = match.clubId, userId = userId)
        return matchSignupRepository.findAllByMatchId(matchId = matchId).map { it.toMatchSignupModel() }
    }

    @Transactional
    fun signUpForMatch(matchId: ClubMatchId, userId: UserId): MatchSignupModel {
        val match = clubMatchRepository.findByIdOrNull(matchId) ?: throw ClubMatchNotFoundException()
        val member = clubMemberRepository.findByClubIdAndUserId(clubId = match.clubId, userId = userId) ?: throw ForbiddenException()
        ensureWithinSignupWindow(match)

        val existing = matchSignupRepository.findByMatchIdAndClubMemberId(matchId = matchId, clubMemberId = requireNotNull(member.id))
        if (existing != null) {
            if (existing.status == SignupStatusEntity.CONFIRMED) throw AlreadySignedUpException()
            existing.status = SignupStatusEntity.CONFIRMED
            return matchSignupRepository.saveAndFlush(existing).toMatchSignupModel()
        }

        val signup = matchSignupRepository.saveAndFlush(
            MatchSignupEntity(matchId = matchId, clubMemberId = member.id)
        )
        return signup.toMatchSignupModel()
    }

    @Transactional
    fun cancelSignup(matchId: ClubMatchId, userId: UserId) {
        val match = clubMatchRepository.findByIdOrNull(matchId) ?: throw ClubMatchNotFoundException()
        val member = clubMemberRepository.findByClubIdAndUserId(clubId = match.clubId, userId = userId) ?: throw ForbiddenException()
        ensureWithinSignupWindow(match)

        val signup = matchSignupRepository.findByMatchIdAndClubMemberId(matchId = matchId, clubMemberId = requireNotNull(member.id))
            ?: throw MatchSignupNotFoundException()
        signup.status = SignupStatusEntity.CANCELLED
        matchSignupRepository.saveAndFlush(signup)
    }

    @Transactional
    fun addGuest(matchId: ClubMatchId, userId: UserId, guestName: String, position: PlayerPositionModel?, rating: Int?): MatchSignupModel {
        val match = clubMatchRepository.findByIdOrNull(matchId) ?: throw ClubMatchNotFoundException()
        ensureCanManageClub(clubId = match.clubId, userId = userId)

        val signup = matchSignupRepository.saveAndFlush(
            MatchSignupEntity(
                matchId = matchId,
                clubMemberId = null,
                guestName = guestName.trim(),
                guestPosition = position?.toEntityPosition(),
                guestRating = rating?.coerceIn(1, 99)
            )
        )
        return signup.toMatchSignupModel()
    }

    @Transactional
    fun removeSignup(matchId: ClubMatchId, userId: UserId, signupId: MatchSignupId) {
        val match = clubMatchRepository.findByIdOrNull(matchId) ?: throw ClubMatchNotFoundException()
        ensureCanManageClub(clubId = match.clubId, userId = userId)

        val signup = matchSignupRepository.findByIdOrNull(signupId)?.takeIf { it.matchId == matchId }
            ?: throw MatchSignupNotFoundException()
        signup.status = SignupStatusEntity.CANCELLED
        matchSignupRepository.saveAndFlush(signup)
    }

    @Transactional
    fun generateTeams(
        matchId: ClubMatchId,
        userId: UserId,
        mode: TeamGenerationMode,
        manualTeamA: List<MatchSignupId>?,
        manualTeamB: List<MatchSignupId>?
    ): ClubMatchModel {
        val match = clubMatchRepository.findByIdOrNull(id = matchId) ?: throw ClubMatchNotFoundException()
        ensureCanManageClub(clubId = match.clubId, userId = userId)

        val confirmedSignups = matchSignupRepository.findAllByMatchIdAndStatus(matchId = matchId, status = SignupStatusEntity.CONFIRMED)
        if (confirmedSignups.size < 2) throw InvalidTeamGenerationRequestException(message = "At least 2 confirmed players are required.")

        val membersById = clubMemberRepository
            .findAllById(confirmedSignups.mapNotNull { it.clubMemberId })
            .associateBy { requireNotNull(it.id) }

        val assignment = when (mode) {
            TeamGenerationMode.AUTO -> autoAssignTeams(confirmedSignups, membersById)
            TeamGenerationMode.MANUAL -> manualAssignTeams(
                confirmedSignups = confirmedSignups.mapNotNull { it.id }.toSet(),
                teamA = manualTeamA,
                teamB = manualTeamB
            )
        }

        matchTeamPlayerRepository.deleteByMatchId(matchId = matchId)
        matchTeamPlayerRepository.saveAll(
            assignment.first.map {
                MatchTeamPlayerEntity(matchId = matchId, matchSignupId = it, teamSide = TEAM_A.toEntitySide())
            } + assignment.second.map {
                MatchTeamPlayerEntity(matchId = matchId, matchSignupId = it, teamSide = TEAM_B.toEntitySide())
            }
        )

        match.status = MatchStatusEntity.TEAMS_GENERATED
        clubMatchRepository.saveAndFlush(match)

        return buildMatchModel(match)
    }

    @Transactional
    fun recordMatchResult(
        matchId: ClubMatchId,
        userId: UserId,
        teamAScore: Int,
        teamBScore: Int,
        playerStats: List<PlayerMatchStatInput>
    ): ClubMatchModel {
        val match = clubMatchRepository.findByIdOrNull(matchId) ?: throw ClubMatchNotFoundException()
        ensureCanManageClub(clubId = match.clubId, userId = userId)
        if (match.status == MatchStatusEntity.COMPLETED) throw MatchAlreadyCompletedException()

        val membersById = clubMemberRepository
            .findAllById(playerStats.map { it.clubMemberId })
            .associateBy { requireNotNull(it.id) }

        playerStats.forEach { stat ->
            val member = membersById[stat.clubMemberId] ?: return@forEach
            if (member.clubId != match.clubId) return@forEach

            member.goals += stat.goals
            member.assists += stat.assists
            member.yellowCards += stat.yellowCards
            member.redCards += stat.redCards
            member.minutesPlayed += stat.minutesPlayed
            member.matchesPlayed += 1
        }
        clubMemberRepository.saveAll(membersById.values)

        match.teamAScore = teamAScore
        match.teamBScore = teamBScore
        match.status = MatchStatusEntity.COMPLETED
        clubMatchRepository.saveAndFlush(match)

        return buildMatchModel(match)
    }

    private fun buildMatchModel(match: ClubMatchEntity): ClubMatchModel {
        val matchId = requireNotNull(match.id)
        val assignments = matchTeamPlayerRepository.findAllByMatchId(matchId = matchId)
        val signupIds = assignments.map { it.matchSignupId }
        val signupsById = matchSignupRepository.findAllByIdIn(signupIds).associateBy { requireNotNull(it.id) }
        val membersById = clubMemberRepository
            .findAllById(signupsById.values.mapNotNull { it.clubMemberId })
            .associateBy { requireNotNull(it.id) }
        val usernamesByMember = membersById.values
            .mapNotNull { member -> member.userParticipant?.let { member.id to it.username } }
            .toMap()

        val teamA = assignments
            .filter { it.teamSide == MatchTeamPlayerEntity.TeamSideEntity.TEAM_A }
            .mapNotNull { signupsById[it.matchSignupId] }
            .map { signup -> signup.toMatchParticipantModel(member = membersById[signup.clubMemberId], username = usernamesByMember[signup.clubMemberId]) }

        val teamB = assignments
            .filter { it.teamSide == MatchTeamPlayerEntity.TeamSideEntity.TEAM_B }
            .mapNotNull { signupsById[it.matchSignupId] }
            .map { signup -> signup.toMatchParticipantModel(member = membersById[signup.clubMemberId], username = usernamesByMember[signup.clubMemberId]) }

        return match.toClubMatchModel(teamA = teamA, teamB = teamB)
    }

    private fun ensureWithinSignupWindow(match: ClubMatchEntity) {
        if (match.status != MatchStatusEntity.SCHEDULED) throw SignupWindowClosedException()
        val now = Instant.now()
        if (now.isBefore(match.signupOpensAt) || !now.isBefore(match.signupClosesAt)) throw SignupWindowClosedException()
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

    private fun autoAssignTeams(
        signups: List<MatchSignupEntity>,
        membersById: Map<ClubMemberId, ClubMemberEntity>
    ): Pair<List<MatchSignupId>, List<MatchSignupId>> {
        val draftPlayers = signups.map { signup ->
            val member = signup.clubMemberId?.let { membersById[it] }
            DraftPlayer(
                signupId = requireNotNull(signup.id),
                position = (member?.position ?: signup.guestPosition)?.toPlayerPositionModel(),
                rating = member?.rating ?: signup.guestRating ?: 50
            )
        }
        return BalancedTeamDrawer.draw(draftPlayers)
    }

    private fun manualAssignTeams(
        confirmedSignups: Set<MatchSignupId>,
        teamA: List<MatchSignupId>?,
        teamB: List<MatchSignupId>?
    ): Pair<List<MatchSignupId>, List<MatchSignupId>> {
        val safeTeamA = teamA?.distinct() ?: emptyList()
        val safeTeamB = teamB?.distinct() ?: emptyList()
        if (safeTeamA.isEmpty() || safeTeamB.isEmpty()) {
            throw InvalidTeamGenerationRequestException(message = "Manual mode requires non-empty teamA and teamB.")
        }

        val allManualSignups = safeTeamA + safeTeamB
        if (allManualSignups.size != allManualSignups.toSet().size) {
            throw InvalidTeamGenerationRequestException(message = "A player cannot be in both teams.")
        }
        if (!confirmedSignups.containsAll(allManualSignups)) {
            throw InvalidTeamGenerationRequestException(message = "All players must be confirmed for this match.")
        }
        if (kotlin.math.abs(safeTeamA.size - safeTeamB.size) > 1) {
            throw InvalidTeamGenerationRequestException(message = "Team sizes must be balanced (difference <= 1).")
        }

        return safeTeamA to safeTeamB
    }

    data class PlayerMatchStatInput(
        val clubMemberId: ClubMemberId,
        val goals: Int,
        val assists: Int,
        val yellowCards: Int,
        val redCards: Int,
        val minutesPlayed: Int
    )

    enum class TeamGenerationMode {
        AUTO,
        MANUAL
    }
}
