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
    val confirmedEntries: List<MatchAnnouncementEntryModel> get() = entries.filter { it.status == MatchAnnouncementEntryModel.EntryStatus.CONFIRMED }
    val waitlistEntries: List<MatchAnnouncementEntryModel> get() = entries.filter { it.status == MatchAnnouncementEntryModel.EntryStatus.WAITLISTED }

    fun isOpenAt(now: Instant): Boolean =
        status == MatchAnnouncementStatus.OPEN && !now.isBefore(opensAt) && now.isBefore(closesAt)

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
    val status: EntryStatus,
    val enrolledAt: Instant
) {
    enum class EntryStatus {
        CONFIRMED,
        WAITLISTED
    }
}
