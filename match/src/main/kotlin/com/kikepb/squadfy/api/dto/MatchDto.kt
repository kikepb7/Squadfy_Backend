package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchId
import java.time.Instant

data class MatchDto(
    val id: MatchId,
    val clubId: ClubId,
    val scheduledAt: Instant,
    val status: MatchStatus,
    val enrolledPlayers: List<ClubMemberId>,
    val teamA: List<ClubMemberId>,
    val teamB: List<ClubMemberId>,
    val durationMinutes: Int,
    /** Effective minutes of every team player. */
    val minutesPlayed: Map<ClubMemberId, Int>,
    /** Confirmed guests of the announcement and guests in each team. */
    val enrolledGuests: List<MatchGuestDto>,
    val teamAGuests: List<MatchGuestDto>,
    val teamBGuests: List<MatchGuestDto>,
    /** True when the score was set manually (official result). */
    val isManualScore: Boolean,
    /** Rating variation of each member once completed ("match rating"). */
    val ratingChanges: Map<ClubMemberId, Int>,
    /** Local date of the weekly schedule occurrence; null for extra matches. */
    val scheduleDate: java.time.LocalDate?,
    val teamAScore: Int,
    val teamBScore: Int,
    val goals: List<MatchEventDto>,
    val assists: List<MatchEventDto>,
    val yellowCards: List<MatchEventDto>,
    val redCards: List<MatchEventDto>,
    val createdAt: Instant,
    val updatedAt: Instant
)
