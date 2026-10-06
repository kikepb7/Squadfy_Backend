package com.kikepb.squadfy.api.mappers

import com.kikepb.squadfy.api.dto.ClubDto
import com.kikepb.squadfy.api.dto.ClubMemberDto
import com.kikepb.squadfy.domain.model.ClubMemberModel
import com.kikepb.squadfy.domain.model.ClubModel

fun ClubModel.toClubDto(): ClubDto = ClubDto(
    id = id,
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

fun ClubMemberModel.toClubMemberDto(): ClubMemberDto = ClubMemberDto(
    id = id,
    clubId = clubId,
    userId = userId,
    username = username,
    profilePictureUrl = profilePictureUrl,
    shirtNumber = shirtNumber,
    position = position,
    goalsScored = goalsScored,
    assists = assists,
    yellowCards = yellowCards,
    redCards = redCards,
    minutesPlayed = minutesPlayed,
    matchesPlayed = matchesPlayed,
    role = role,
    createdAt = createdAt,
    updatedAt = updatedAt
)
