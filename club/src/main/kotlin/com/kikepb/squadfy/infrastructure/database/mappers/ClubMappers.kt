package com.kikepb.squadfy.infrastructure.database.mappers

import com.kikepb.squadfy.domain.model.ClubMemberModel
import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole
import com.kikepb.squadfy.domain.model.ClubModel
import com.kikepb.squadfy.domain.model.ClubParticipantModel
import com.kikepb.squadfy.infrastructure.database.entities.ClubEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity.ClubMemberRoleEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity.ClubMemberRoleEntity.ADMIN
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity.ClubMemberRoleEntity.CAPTAIN
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity.ClubMemberRoleEntity.OWNER
import com.kikepb.squadfy.infrastructure.database.entities.ClubMemberEntity.ClubMemberRoleEntity.PLAYER
import com.kikepb.squadfy.infrastructure.database.entities.ClubParticipantEntity

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
        position = position,
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

fun ClubMemberRoleEntity.toClubMemberRole(): ClubMemberRole =
    when (this) {
        OWNER   -> ClubMemberRole.OWNER
        ADMIN   -> ClubMemberRole.ADMIN
        CAPTAIN -> ClubMemberRole.CAPTAIN
        PLAYER  -> ClubMemberRole.PLAYER
    }

fun ClubMemberRole.toEntityRole(): ClubMemberRoleEntity =
    when (this) {
        ClubMemberRole.OWNER   -> OWNER
        ClubMemberRole.ADMIN   -> ADMIN
        ClubMemberRole.CAPTAIN -> CAPTAIN
        ClubMemberRole.PLAYER  -> PLAYER
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
