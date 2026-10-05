package com.kikepb.squadfy.infrastructure.database.mappers

import com.kikepb.squadfy.domain.model.MatchAnnouncementEntryModel
import com.kikepb.squadfy.domain.model.MatchAnnouncementModel
import com.kikepb.squadfy.domain.model.ClubMatchScheduleModel
import com.kikepb.squadfy.domain.model.MatchEventModel
import com.kikepb.squadfy.domain.model.MatchModel
import com.kikepb.squadfy.domain.model.TeamSideModel
import com.kikepb.squadfy.domain.model.TeamSideModel.TEAM_A
import com.kikepb.squadfy.domain.model.TeamSideModel.TEAM_B
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.infrastructure.database.entities.MatchAnnouncementEntryEntity
import com.kikepb.squadfy.infrastructure.database.entities.MatchAnnouncementEntity
import com.kikepb.squadfy.infrastructure.database.entities.ClubMatchScheduleEntity
import com.kikepb.squadfy.infrastructure.database.entities.MatchEntity
import com.kikepb.squadfy.infrastructure.database.entities.MatchEventEntity
import com.kikepb.squadfy.infrastructure.database.entities.MatchTeamPlayerEntity
import com.kikepb.squadfy.infrastructure.database.entities.MatchTeamPlayerEntity.TeamSideEntity
import java.time.ZoneId

fun MatchEntity.toMatchModel(
    players: List<MatchTeamPlayerEntity> = emptyList(),
    events: List<MatchEventEntity> = emptyList(),
    enrolledPlayers: List<ClubMemberId> = emptyList()
): MatchModel =
    MatchModel(
        id = requireNotNull(id),
        clubId = clubId,
        scheduledAt = scheduledAt,
        status = status,
        enrolledPlayers = enrolledPlayers,
        teamA = players.filter { it.teamSide == TeamSideEntity.TEAM_A }.map { it.clubMemberId },
        teamB = players.filter { it.teamSide == TeamSideEntity.TEAM_B }.map { it.clubMemberId },
        events = events.map { it.toMatchEventModel() },
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun MatchEventEntity.toMatchEventModel(): MatchEventModel =
    MatchEventModel(
        id = requireNotNull(id),
        matchId = matchId,
        clubMemberId = clubMemberId,
        type = type,
        minute = minute,
        createdAt = createdAt
    )

fun TeamSideModel.toEntitySide(): TeamSideEntity =
    when (this) {
        TEAM_A -> TeamSideEntity.TEAM_A
        TEAM_B -> TeamSideEntity.TEAM_B
    }

fun ClubMatchScheduleEntity.toClubMatchScheduleModel(): ClubMatchScheduleModel =
    ClubMatchScheduleModel(
        id = requireNotNull(id),
        clubId = clubId,
        matchDayOfWeek = matchDayOfWeek,
        matchTime = matchTime,
        timeZone = ZoneId.of(timeZone),
        format = format,
        maxPlayers = maxPlayers,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun MatchAnnouncementEntity.toMatchAnnouncementModel(entries: List<MatchAnnouncementEntryEntity>): MatchAnnouncementModel =
    MatchAnnouncementModel(
        id = requireNotNull(id),
        matchId = matchId,
        clubId = clubId,
        maxPlayers = maxPlayers,
        opensAt = opensAt,
        closesAt = closesAt,
        status = status,
        entries = entries.map { it.toMatchAnnouncementEntryModel() },
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun MatchAnnouncementEntryEntity.toMatchAnnouncementEntryModel(): MatchAnnouncementEntryModel =
    MatchAnnouncementEntryModel(
        id = requireNotNull(id),
        matchAnnouncementId = matchAnnouncementId,
        clubMemberId = clubMemberId,
        status = status,
        enrolledAt = enrolledAt
    )
