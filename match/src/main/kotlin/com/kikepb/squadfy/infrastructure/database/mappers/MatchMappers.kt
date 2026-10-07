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
import com.kikepb.squadfy.domain.club.PlayerPosition
import com.kikepb.squadfy.domain.model.DeadlineRule
import com.kikepb.squadfy.domain.model.MatchGuestModel
import com.kikepb.squadfy.domain.model.MemberAbsenceModel
import com.kikepb.squadfy.domain.model.ScheduleExceptionModel
import com.kikepb.squadfy.infrastructure.database.entities.MemberAbsenceEntity
import com.kikepb.squadfy.infrastructure.database.entities.ScheduleExceptionEntity
import java.util.UUID

fun MatchEntity.toMatchModel(
    players: List<MatchTeamPlayerEntity> = emptyList(),
    events: List<MatchEventEntity> = emptyList(),
    enrolledPlayers: List<ClubMemberId> = emptyList(),
    enrolledGuests: List<MatchAnnouncementEntryEntity> = emptyList(),
    guestEntries: Map<UUID, MatchAnnouncementEntryEntity> = emptyMap(),
    ratingChanges: Map<ClubMemberId, Double> = emptyMap()
): MatchModel {
    val members = players.filter { it.clubMemberId != null }
    fun guestsOf(side: TeamSideEntity) = players
        .filter { it.teamSide == side && it.guestEntryId != null }
        .mapNotNull { guestEntries[it.guestEntryId]?.toMatchGuestModel() }

    return MatchModel(
        id = requireNotNull(id),
        clubId = clubId,
        scheduledAt = scheduledAt,
        status = status,
        enrolledPlayers = enrolledPlayers,
        teamA = members.filter { it.teamSide == TeamSideEntity.TEAM_A }.map { requireNotNull(it.clubMemberId) },
        teamB = members.filter { it.teamSide == TeamSideEntity.TEAM_B }.map { requireNotNull(it.clubMemberId) },
        events = events.map { it.toMatchEventModel() },
        durationMinutes = durationMinutes,
        minutesPlayed = members.associate { requireNotNull(it.clubMemberId) to (it.minutesPlayed ?: durationMinutes) },
        enrolledGuests = enrolledGuests.map { it.toMatchGuestModel() },
        teamAGuests = guestsOf(TeamSideEntity.TEAM_A),
        teamBGuests = guestsOf(TeamSideEntity.TEAM_B),
        manualTeamAScore = manualTeamAScore,
        manualTeamBScore = manualTeamBScore,
        ratingChanges = ratingChanges,
        scheduleDate = scheduleDate,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun MatchAnnouncementEntryEntity.toMatchGuestModel(): MatchGuestModel =
    MatchGuestModel(
        guestId = requireNotNull(id),
        name = guestName.orEmpty(),
        position = PlayerPosition.fromRaw(guestPosition),
        invitedByMemberId = invitedByMemberId
    )

fun ScheduleExceptionEntity.toScheduleExceptionModel(): ScheduleExceptionModel =
    ScheduleExceptionModel(
        id = requireNotNull(id),
        clubId = clubId,
        scheduleDate = scheduleDate,
        type = type,
        newScheduledAt = newScheduledAt,
        reason = reason,
        createdAt = createdAt
    )

fun MemberAbsenceEntity.toMemberAbsenceModel(): MemberAbsenceModel =
    MemberAbsenceModel(
        id = requireNotNull(id),
        clubId = clubId,
        clubMemberId = clubMemberId,
        fromDate = fromDate,
        toDate = toDate,
        reason = reason,
        createdAt = createdAt
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
        matchDurationMinutes = matchDurationMinutes,
        close = DeadlineRule(daysBefore = closeDaysBefore, time = closeTime),
        draw = DeadlineRule(daysBefore = drawDaysBefore, time = drawTime),
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
        drawAt = drawAt,
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
        enrolledAt = enrolledAt,
        participantType = participantType,
        guestName = guestName,
        guestPosition = PlayerPosition.fromRaw(guestPosition),
        invitedByMemberId = invitedByMemberId
    )
