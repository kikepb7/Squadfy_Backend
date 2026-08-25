package com.kikepb.squadfy.infrastructure.database.mappers

import com.kikepb.squadfy.domain.model.ClubMatchModel
import com.kikepb.squadfy.domain.model.ClubMemberModel
import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole
import com.kikepb.squadfy.domain.model.ClubModel
import com.kikepb.squadfy.domain.model.ClubParticipantModel
import com.kikepb.squadfy.domain.model.ClubScheduleExceptionModel
import com.kikepb.squadfy.domain.model.MatchParticipantModel
import com.kikepb.squadfy.domain.model.MatchPlayerStatModel
import com.kikepb.squadfy.domain.model.MatchSignupModel
import com.kikepb.squadfy.domain.model.MatchStatusModel
import com.kikepb.squadfy.domain.model.PlayerPositionModel
import com.kikepb.squadfy.domain.model.SignupStatusModel
import com.kikepb.squadfy.domain.model.TeamSideModel
import com.kikepb.squadfy.infrastructure.database.entities.ClubEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMatchEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMatchEntity.MatchStatusEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity.ClubMemberRoleEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity.ClubMemberRoleEntity.ADMIN
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity.ClubMemberRoleEntity.CAPTAIN
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity.ClubMemberRoleEntity.OWNER
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity.ClubMemberRoleEntity.PLAYER
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity.PlayerPositionEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubParticipantEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubScheduleExceptionEntity
import com.kikepb.squadfy.infrastructure.database.entities.MatchPlayerStatEntity
import com.kikepb.squadfy.infrastructure.database.entities.MatchSignupEntity
import com.kikepb.squadfy.infrastructure.database.entities.MatchSignupEntity.SignupStatusEntity
import com.kikepb.squadfy.infrastructure.database.entities.MatchTeamPlayerEntity
import com.kikepb.squadfy.infrastructure.database.entities.MatchTeamPlayerEntity.TeamSideEntity
import com.kikepb.squadfy.infrastructure.database.entities.MatchTeamPlayerEntity.TeamSideEntity.TEAM_A
import com.kikepb.squadfy.infrastructure.database.entities.MatchTeamPlayerEntity.TeamSideEntity.TEAM_B

fun ClubEntity.toClubModel(membersCount: Int): ClubModel =
    ClubModel(
        id = requireNotNull(id),
        name = name,
        description = description,
        clubLogoUrl = clubLogoUrl,
        ownerId = ownerId,
        invitationCode = invitationCode,
        maxMembers = maxMembers,
        membersCount = membersCount,
        matchDayOfWeek = matchDayOfWeek,
        matchStartTime = matchStartTime,
        matchEndTime = matchEndTime,
        seasonStartMonth = seasonStartMonth,
        seasonStartDay = seasonStartDay,
        drawTime = drawTime,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun ClubMemberEntity.toClubMemberModel(username: String, email: String, profilePictureUrl: String?): ClubMemberModel =
    ClubMemberModel(
        id = requireNotNull(id),
        clubId = clubId,
        userId = userId,
        username = username,
        email = email,
        profilePictureUrl = profilePictureUrl,
        shirtNumber = shirtNumber,
        position = position?.toPlayerPositionModel(),
        rating = rating,
        goalsScored = goals,
        assists = assists,
        yellowCards = yellowCards,
        redCards = redCards,
        minutesPlayed = minutesPlayed,
        matchesPlayed = matchesPlayed,
        role = role.toClubMemberRole(),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun ClubMatchEntity.toClubMatchModel(teamA: List<MatchParticipantModel>, teamB: List<MatchParticipantModel>): ClubMatchModel =
    ClubMatchModel(
        id = requireNotNull(id),
        clubId = clubId,
        scheduledAt = scheduledAt,
        signupOpensAt = signupOpensAt,
        signupClosesAt = signupClosesAt,
        status = status.toMatchStatusModel(),
        teamAScore = teamAScore,
        teamBScore = teamBScore,
        teamA = teamA,
        teamB = teamB,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

/**
 * A signup's display name/position/rating come from the club member snapshot when it belongs to
 * a full member, or from the guest fields captured at sign-up time otherwise.
 */
fun MatchSignupEntity.toMatchParticipantModel(member: ClubMemberEntity?, username: String?): MatchParticipantModel =
    MatchParticipantModel(
        signupId = requireNotNull(id),
        clubMemberId = clubMemberId,
        displayName = username ?: guestName ?: "Guest",
        position = member?.position?.toPlayerPositionModel() ?: guestPosition?.toPlayerPositionModel(),
        rating = member?.rating ?: guestRating ?: 50
    )

fun MatchSignupEntity.toMatchSignupModel(): MatchSignupModel =
    MatchSignupModel(
        id = requireNotNull(id),
        matchId = matchId,
        clubMemberId = clubMemberId,
        guestName = guestName,
        position = guestPosition?.toPlayerPositionModel(),
        rating = guestRating,
        status = status.toSignupStatusModel(),
        signedUpAt = signedUpAt
    )

fun MatchPlayerStatEntity.toMatchPlayerStatModel(): MatchPlayerStatModel =
    MatchPlayerStatModel(
        id = requireNotNull(id),
        matchId = matchId,
        clubMemberId = clubMemberId,
        goals = goals,
        assists = assists,
        yellowCards = yellowCards,
        redCards = redCards,
        minutesPlayed = minutesPlayed
    )

fun ClubScheduleExceptionEntity.toClubScheduleExceptionModel(): ClubScheduleExceptionModel =
    ClubScheduleExceptionModel(
        id = requireNotNull(id),
        clubId = clubId,
        date = date,
        reason = reason,
        createdAt = createdAt
    )

fun ClubMemberRoleEntity.toClubMemberRole(): ClubMemberRole =
    when (this) {
        OWNER -> ClubMemberRole.OWNER
        ADMIN -> ClubMemberRole.ADMIN
        CAPTAIN -> ClubMemberRole.CAPTAIN
        PLAYER -> ClubMemberRole.PLAYER
    }

fun ClubMemberRole.toEntityRole(): ClubMemberRoleEntity =
    when (this) {
        ClubMemberRole.OWNER -> OWNER
        ClubMemberRole.ADMIN -> ADMIN
        ClubMemberRole.CAPTAIN -> CAPTAIN
        ClubMemberRole.PLAYER -> PLAYER
    }

fun PlayerPositionEntity.toPlayerPositionModel(): PlayerPositionModel =
    when (this) {
        PlayerPositionEntity.GOALKEEPER -> PlayerPositionModel.GOALKEEPER
        PlayerPositionEntity.DEFENDER -> PlayerPositionModel.DEFENDER
        PlayerPositionEntity.MIDFIELDER -> PlayerPositionModel.MIDFIELDER
        PlayerPositionEntity.FORWARD -> PlayerPositionModel.FORWARD
    }

fun PlayerPositionModel.toEntityPosition(): PlayerPositionEntity =
    when (this) {
        PlayerPositionModel.GOALKEEPER -> PlayerPositionEntity.GOALKEEPER
        PlayerPositionModel.DEFENDER -> PlayerPositionEntity.DEFENDER
        PlayerPositionModel.MIDFIELDER -> PlayerPositionEntity.MIDFIELDER
        PlayerPositionModel.FORWARD -> PlayerPositionEntity.FORWARD
    }

fun MatchStatusEntity.toMatchStatusModel(): MatchStatusModel =
    when (this) {
        MatchStatusEntity.SCHEDULED -> MatchStatusModel.SCHEDULED
        MatchStatusEntity.TEAMS_GENERATED -> MatchStatusModel.TEAMS_GENERATED
        MatchStatusEntity.COMPLETED -> MatchStatusModel.COMPLETED
        MatchStatusEntity.CANCELLED -> MatchStatusModel.CANCELLED
    }

fun SignupStatusEntity.toSignupStatusModel(): SignupStatusModel =
    when (this) {
        SignupStatusEntity.CONFIRMED -> SignupStatusModel.CONFIRMED
        SignupStatusEntity.CANCELLED -> SignupStatusModel.CANCELLED
    }

fun TeamSideModel.toEntitySide(): TeamSideEntity =
    when (this) {
        TeamSideModel.TEAM_A -> TEAM_A
        TeamSideModel.TEAM_B -> TEAM_B
    }

fun TeamSideEntity.toTeamSideModel(): TeamSideModel =
    when (this) {
        TEAM_A -> TeamSideModel.TEAM_A
        TEAM_B -> TeamSideModel.TEAM_B
    }

fun ClubParticipantModel.toClubParticipantEntity(): ClubParticipantEntity =
    ClubParticipantEntity(
        userId = userId,
        username = username,
        email = email,
        profilePictureUrl = profilePictureUrl
    )

fun ClubParticipantEntity.toClubParticipantModel(): ClubParticipantModel =
    ClubParticipantModel(
        userId = userId,
        username = username,
        email = email,
        profilePictureUrl = profilePictureUrl
    )
