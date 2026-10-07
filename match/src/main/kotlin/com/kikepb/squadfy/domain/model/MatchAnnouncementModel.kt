package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.MatchAnnouncementEntryId
import com.kikepb.squadfy.domain.type.MatchAnnouncementId
import com.kikepb.squadfy.domain.club.PlayerPosition
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
    val drawAt: Instant,
    val status: MatchAnnouncementStatus,
    val entries: List<MatchAnnouncementEntryModel>,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    /** Members first, then guests, each by enrollment time (spec 008 RN-A2/A3). */
    val confirmedEntries: List<MatchAnnouncementEntryModel> get() = inPriorityOrder(MatchAnnouncementEntryModel.EntryStatus.CONFIRMED)
    val waitlistEntries: List<MatchAnnouncementEntryModel> get() = inPriorityOrder(MatchAnnouncementEntryModel.EntryStatus.WAITLISTED)

    private fun inPriorityOrder(status: MatchAnnouncementEntryModel.EntryStatus) =
        entries.filter { it.status == status }.sortedWith(compareBy({ it.isGuest }, { it.enrolledAt }))

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
    /** Null for guests. */
    val clubMemberId: ClubMemberId?,
    val status: EntryStatus,
    val enrolledAt: Instant,
    val participantType: ParticipantType = ParticipantType.MEMBER,
    val guestName: String? = null,
    val guestPosition: PlayerPosition? = null,
    val invitedByMemberId: ClubMemberId? = null
) {
    val isGuest: Boolean get() = participantType == ParticipantType.GUEST

    enum class ParticipantType {
        MEMBER,
        GUEST
    }

    enum class EntryStatus {
        CONFIRMED,
        WAITLISTED
    }
}
