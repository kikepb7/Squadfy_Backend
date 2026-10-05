package com.kikepb.squadfy.api.mappers

import com.kikepb.squadfy.api.dto.MatchAnnouncementDto
import com.kikepb.squadfy.api.dto.MatchAnnouncementEntryDto
import com.kikepb.squadfy.api.dto.ClubMatchScheduleDto
import com.kikepb.squadfy.api.dto.MatchDto
import com.kikepb.squadfy.api.dto.MatchEventDto
import com.kikepb.squadfy.domain.model.MatchAnnouncementEntryModel
import com.kikepb.squadfy.domain.model.MatchAnnouncementModel
import com.kikepb.squadfy.domain.model.ClubMatchScheduleModel
import com.kikepb.squadfy.domain.model.MatchEventModel
import com.kikepb.squadfy.domain.model.MatchEventType.ASSIST
import com.kikepb.squadfy.domain.model.MatchEventType.GOAL
import com.kikepb.squadfy.domain.model.MatchEventType.RED_CARD
import com.kikepb.squadfy.domain.model.MatchEventType.YELLOW_CARD
import com.kikepb.squadfy.domain.model.MatchModel

fun MatchModel.toMatchDto(): MatchDto = MatchDto(
    id = id,
    clubId = clubId,
    scheduledAt = scheduledAt,
    status = status,
    enrolledPlayers = enrolledPlayers,
    teamA = teamA,
    teamB = teamB,
    teamAScore = teamAScore,
    teamBScore = teamBScore,
    goals = events.filter { it.type == GOAL }.map { it.toMatchEventDto() },
    assists = events.filter { it.type == ASSIST }.map { it.toMatchEventDto() },
    yellowCards = events.filter { it.type == YELLOW_CARD }.map { it.toMatchEventDto() },
    redCards = events.filter { it.type == RED_CARD }.map { it.toMatchEventDto() },
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MatchEventModel.toMatchEventDto(): MatchEventDto = MatchEventDto(
    id = id,
    matchId = matchId,
    clubMemberId = clubMemberId,
    type = type,
    minute = minute,
    createdAt = createdAt
)

fun ClubMatchScheduleModel.toClubMatchScheduleDto(): ClubMatchScheduleDto = ClubMatchScheduleDto(
    id = id,
    clubId = clubId,
    matchDayOfWeek = matchDayOfWeek,
    matchTime = matchTime,
    matchAnnouncementOpenDaysBeforeMatch = matchAnnouncementOpenDaysBeforeMatch,
    maxPlayers = maxPlayers,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MatchAnnouncementModel.toMatchAnnouncementDto(): MatchAnnouncementDto = MatchAnnouncementDto(
    id = id,
    matchId = matchId,
    clubId = clubId,
    maxPlayers = maxPlayers,
    enrolledCount = enrolledCount,
    opensAt = opensAt,
    closesAt = closesAt,
    status = status,
    entries = entries.map { it.toMatchAnnouncementEntryDto() },
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MatchAnnouncementEntryModel.toMatchAnnouncementEntryDto(): MatchAnnouncementEntryDto = MatchAnnouncementEntryDto(
    id = id,
    matchAnnouncementId = matchAnnouncementId,
    clubMemberId = clubMemberId,
    enrolledAt = enrolledAt
)
