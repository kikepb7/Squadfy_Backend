package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.model.MatchAnnouncementModel.MatchAnnouncementStatus
import com.kikepb.squadfy.domain.type.MatchAnnouncementId
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.MatchId
import java.time.Instant

data class MatchAnnouncementDto(
    val id: MatchAnnouncementId,
    val matchId: MatchId,
    val clubId: ClubId,
    val maxPlayers: Int,
    val enrolledCount: Int,
    val opensAt: Instant,
    val closesAt: Instant,
    val status: MatchAnnouncementStatus,
    val entries: List<MatchAnnouncementEntryDto>,
    val createdAt: Instant,
    val updatedAt: Instant
)
