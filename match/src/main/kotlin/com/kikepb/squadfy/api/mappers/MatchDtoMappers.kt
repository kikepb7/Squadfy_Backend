package com.kikepb.squadfy.api.mappers

import com.kikepb.squadfy.api.dto.MatchAnnouncementDto
import com.kikepb.squadfy.api.dto.MatchAnnouncementEntryDto
import com.kikepb.squadfy.api.dto.ClubMatchScheduleDto
import com.kikepb.squadfy.api.dto.ClubStatsEntryDto
import com.kikepb.squadfy.api.dto.PlayerStatsDto
import com.kikepb.squadfy.api.dto.CurrentMatchAnnouncementDto
import com.kikepb.squadfy.api.dto.MatchDto
import com.kikepb.squadfy.api.dto.MatchEventDto
import com.kikepb.squadfy.api.dto.MatchGuestDto
import com.kikepb.squadfy.api.dto.MemberAbsenceDto
import com.kikepb.squadfy.api.dto.ScheduleExceptionDto
import com.kikepb.squadfy.api.dto.PlayerRatingDto
import com.kikepb.squadfy.api.dto.RatingLeaderboardEntryDto
import com.kikepb.squadfy.api.dto.TeamBalanceDto
import com.kikepb.squadfy.api.dto.TeamPlayerRatingDto
import com.kikepb.squadfy.api.dto.TeamStrengthDto
import com.kikepb.squadfy.domain.model.MatchAnnouncementEntryModel
import com.kikepb.squadfy.domain.model.MatchAnnouncementModel
import com.kikepb.squadfy.domain.model.ClubMatchScheduleModel
import com.kikepb.squadfy.domain.model.CurrentMatchAnnouncementModel
import com.kikepb.squadfy.domain.model.MatchEventModel
import com.kikepb.squadfy.domain.model.MatchEventType.ASSIST
import com.kikepb.squadfy.domain.model.MatchEventType.GOAL
import com.kikepb.squadfy.domain.model.MatchEventType.RED_CARD
import com.kikepb.squadfy.domain.model.MatchEventType.YELLOW_CARD
import com.kikepb.squadfy.domain.model.MatchGuestModel
import com.kikepb.squadfy.domain.model.MatchModel
import com.kikepb.squadfy.domain.model.MemberAbsenceModel
import com.kikepb.squadfy.domain.model.ScheduleExceptionModel
import com.kikepb.squadfy.domain.model.PlayerRatingModel
import com.kikepb.squadfy.domain.model.PlayerStatsModel
import com.kikepb.squadfy.domain.model.StatsLeaderboard
import com.kikepb.squadfy.domain.model.RatingLeaderboard
import com.kikepb.squadfy.domain.model.TeamBalanceModel
import kotlin.math.roundToInt

fun MatchModel.toMatchDto(): MatchDto = MatchDto(
    id = id,
    clubId = clubId,
    scheduledAt = scheduledAt,
    status = status,
    enrolledPlayers = enrolledPlayers,
    teamA = teamA,
    teamB = teamB,
    durationMinutes = durationMinutes,
    minutesPlayed = minutesPlayed,
    enrolledGuests = enrolledGuests.map { it.toMatchGuestDto() },
    teamAGuests = teamAGuests.map { it.toMatchGuestDto() },
    teamBGuests = teamBGuests.map { it.toMatchGuestDto() },
    isManualScore = isManualScore,
    ratingChanges = ratingChanges.mapValues { (_, delta) -> delta.roundToInt() },
    scheduleDate = scheduleDate,
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
    timeZone = timeZone.id,
    format = format,
    maxPlayers = maxPlayers,
    matchDurationMinutes = matchDurationMinutes,
    closeDaysBefore = close.daysBefore,
    closeTime = close.time,
    drawDaysBefore = draw.daysBefore,
    drawTime = draw.time,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MatchAnnouncementModel.toMatchAnnouncementDto(): MatchAnnouncementDto = MatchAnnouncementDto(
    id = id,
    matchId = matchId,
    clubId = clubId,
    maxPlayers = maxPlayers,
    confirmedCount = confirmedEntries.size,
    waitlistCount = waitlistEntries.size,
    opensAt = opensAt,
    closesAt = closesAt,
    drawAt = drawAt,
    status = status,
    entries = confirmedEntries.map { it.toMatchAnnouncementEntryDto() },
    waitlist = waitlistEntries.map { it.toMatchAnnouncementEntryDto() },
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MatchAnnouncementEntryModel.toMatchAnnouncementEntryDto(): MatchAnnouncementEntryDto = MatchAnnouncementEntryDto(
    id = id,
    matchAnnouncementId = matchAnnouncementId,
    participantType = participantType,
    clubMemberId = clubMemberId,
    guestName = guestName,
    guestPosition = guestPosition,
    invitedByMemberId = invitedByMemberId,
    status = status,
    enrolledAt = enrolledAt
)

fun PlayerRatingModel.toPlayerRatingDto(): PlayerRatingDto = PlayerRatingDto(
    clubId = clubId,
    clubMemberId = clubMemberId,
    rating = rating.roundToInt(),
    matchesRated = matchesRated,
    isProvisional = isProvisional,
    rank = rank,
    totalPlayers = totalPlayers
)

fun RatingLeaderboard.RankedEntry.toRatingLeaderboardEntryDto(): RatingLeaderboardEntryDto = RatingLeaderboardEntryDto(
    rank = rank,
    clubMemberId = clubMemberId,
    rating = rating.roundToInt(),
    matchesRated = matchesRated,
    isProvisional = isProvisional
)

fun TeamBalanceModel.toTeamBalanceDto(): TeamBalanceDto = TeamBalanceDto(
    matchId = matchId,
    teamA = teamA.toTeamStrengthDto(),
    teamB = teamB.toTeamStrengthDto(),
    averageRatingDifference = averageRatingDifference.roundToInt(),
    teamAExpectedScore = (teamAExpectedScore * 100).roundToInt() / 100.0
)

private fun TeamBalanceModel.TeamStrength.toTeamStrengthDto(): TeamStrengthDto = TeamStrengthDto(
    players = players,
    averageRating = averageRating.roundToInt(),
    totalRating = totalRating.roundToInt(),
    playerRatings = playerRatings.map { TeamPlayerRatingDto(clubMemberId = it.clubMemberId, rating = it.rating.roundToInt(), isGuest = it.isGuest) }
)

fun CurrentMatchAnnouncementModel.toCurrentMatchAnnouncementDto(): CurrentMatchAnnouncementDto = CurrentMatchAnnouncementDto(
    announcement = announcement.toMatchAnnouncementDto(),
    matchScheduledAt = matchScheduledAt,
    myStatus = myStatus,
    myWaitlistPosition = myWaitlistPosition
)

fun PlayerStatsModel.toPlayerStatsDto(): PlayerStatsDto = PlayerStatsDto(
    clubMemberId = clubMemberId,
    matchesPlayed = matchesPlayed,
    wins = wins,
    draws = draws,
    losses = losses,
    goals = goals,
    assists = assists,
    yellowCards = yellowCards,
    redCards = redCards,
    minutesPlayed = minutesPlayed
)

fun StatsLeaderboard.RankedStats.toClubStatsEntryDto(): ClubStatsEntryDto = ClubStatsEntryDto(
    rank = rank,
    clubMemberId = stats.clubMemberId,
    matchesPlayed = stats.matchesPlayed,
    wins = stats.wins,
    draws = stats.draws,
    losses = stats.losses,
    goals = stats.goals,
    assists = stats.assists,
    yellowCards = stats.yellowCards,
    redCards = stats.redCards,
    minutesPlayed = stats.minutesPlayed
)

fun MatchGuestModel.toMatchGuestDto(): MatchGuestDto = MatchGuestDto(
    guestId = guestId,
    name = name,
    position = position,
    invitedByMemberId = invitedByMemberId
)

fun ScheduleExceptionModel.toScheduleExceptionDto(): ScheduleExceptionDto = ScheduleExceptionDto(
    id = id,
    clubId = clubId,
    date = scheduleDate,
    type = type,
    newScheduledAt = newScheduledAt,
    reason = reason,
    createdAt = createdAt
)

fun MemberAbsenceModel.toMemberAbsenceDto(): MemberAbsenceDto = MemberAbsenceDto(
    id = id,
    clubId = clubId,
    clubMemberId = clubMemberId,
    fromDate = fromDate,
    toDate = toDate,
    reason = reason,
    createdAt = createdAt
)
