package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.club.PlayerPosition
import com.kikepb.squadfy.domain.model.MatchAnnouncementEntryModel.EntryStatus
import com.kikepb.squadfy.domain.model.MatchAnnouncementEntryModel.ParticipantType
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.MatchAnnouncementEntryId
import com.kikepb.squadfy.domain.type.MatchAnnouncementId
import java.time.Instant

/** A member or a guest of the announcement. For guests, [id] is the guestId and [clubMemberId] is null. */
data class MatchAnnouncementEntryDto(
    val id: MatchAnnouncementEntryId,
    val matchAnnouncementId: MatchAnnouncementId,
    val participantType: ParticipantType,
    val clubMemberId: ClubMemberId?,
    val guestName: String?,
    val guestPosition: PlayerPosition?,
    val invitedByMemberId: ClubMemberId?,
    val status: EntryStatus,
    val enrolledAt: Instant
)
