package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.MatchAnnouncementEntryId
import com.kikepb.squadfy.domain.type.MatchAnnouncementId
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchId
import java.time.Instant

data class MatchAnnouncementModel(
    val id: MatchAnnouncementId,
    val matchId: MatchId,
    val clubId: ClubId,
    val maxPlayers: Int,
    val opensAt: Instant,
    val closesAt: Instant,
    val status: MatchAnnouncementStatus,
    val entries: List<MatchAnnouncementEntryModel>,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    val enrolledCount: Int get() = entries.size
    val isFull: Boolean get() = enrolledCount >= maxPlayers

    enum class MatchAnnouncementStatus {
        OPEN,
        CLOSED,
        CANCELLED
    }
}

data class MatchAnnouncementEntryModel(
    val id: MatchAnnouncementEntryId,
    val matchAnnouncementId: MatchAnnouncementId,
    val clubMemberId: ClubMemberId,
    val enrolledAt: Instant
)
