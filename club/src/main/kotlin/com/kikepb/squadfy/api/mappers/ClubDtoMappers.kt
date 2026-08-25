package com.kikepb.squadfy.api.mappers

import com.kikepb.squadfy.api.dto.ClubDto
import com.kikepb.squadfy.api.dto.ClubMatchDto
import com.kikepb.squadfy.api.dto.ClubMemberDto
import com.kikepb.squadfy.api.dto.ClubScheduleExceptionDto
import com.kikepb.squadfy.api.dto.MatchParticipantDto
import com.kikepb.squadfy.api.dto.MatchSignupDto
import com.kikepb.squadfy.api.dto.TeamGenerationModeDto
import com.kikepb.squadfy.domain.model.ClubMatchModel
import com.kikepb.squadfy.domain.model.ClubMemberModel
import com.kikepb.squadfy.domain.model.ClubModel
import com.kikepb.squadfy.domain.model.ClubScheduleExceptionModel
import com.kikepb.squadfy.domain.model.MatchParticipantModel
import com.kikepb.squadfy.domain.model.MatchSignupModel
import com.kikepb.squadfy.service.ClubService.TeamGenerationMode

fun ClubModel.toClubDto(): ClubDto = ClubDto(
    id = id,
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

fun ClubMemberModel.toClubMemberDto(): ClubMemberDto = ClubMemberDto(
    id = id,
    clubId = clubId,
    userId = userId,
    username = username,
    email = email,
    profilePictureUrl = profilePictureUrl,
    shirtNumber = shirtNumber,
    position = position,
    rating = rating,
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

fun MatchParticipantModel.toMatchParticipantDto(): MatchParticipantDto = MatchParticipantDto(
    signupId = signupId,
    clubMemberId = clubMemberId,
    displayName = displayName,
    position = position,
    rating = rating
)

fun ClubMatchModel.toClubMatchDto(): ClubMatchDto = ClubMatchDto(
    id = id,
    clubId = clubId,
    scheduledAt = scheduledAt,
    signupOpensAt = signupOpensAt,
    signupClosesAt = signupClosesAt,
    status = status,
    teamAScore = teamAScore,
    teamBScore = teamBScore,
    teamA = teamA.map { it.toMatchParticipantDto() },
    teamB = teamB.map { it.toMatchParticipantDto() },
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MatchSignupModel.toMatchSignupDto(): MatchSignupDto = MatchSignupDto(
    id = id,
    matchId = matchId,
    clubMemberId = clubMemberId,
    guestName = guestName,
    position = position,
    rating = rating,
    status = status,
    signedUpAt = signedUpAt
)

fun ClubScheduleExceptionModel.toClubScheduleExceptionDto(): ClubScheduleExceptionDto = ClubScheduleExceptionDto(
    id = id,
    clubId = clubId,
    date = date,
    reason = reason,
    createdAt = createdAt
)

fun TeamGenerationModeDto.toDomain(): TeamGenerationMode = when (this) {
    TeamGenerationModeDto.AUTO -> TeamGenerationMode.AUTO
    TeamGenerationModeDto.MANUAL -> TeamGenerationMode.MANUAL
}
