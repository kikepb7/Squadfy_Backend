package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.model.MatchAnnouncementModel.MatchAnnouncementStatus
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.MatchAnnouncementId
import com.kikepb.squadfy.domain.type.MatchId
import java.time.Instant

data class MatchAnnouncementDto(
    val id: MatchAnnouncementId,
    val matchId: MatchId,
    val clubId: ClubId,
    val maxPlayers: Int,
    val confirmedCount: Int,
    val waitlistCount: Int,
    val opensAt: Instant,
    val closesAt: Instant,
    val status: MatchAnnouncementStatus,
    /** Confirmed players in enrollment order. */
    val entries: List<MatchAnnouncementEntryDto>,
    /** Waitlisted players in promotion order. */
    val waitlist: List<MatchAnnouncementEntryDto>,
    val createdAt: Instant,
    val updatedAt: Instant
)
