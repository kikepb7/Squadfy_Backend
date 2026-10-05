package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.type.MatchAnnouncementEntryId
import com.kikepb.squadfy.domain.type.MatchAnnouncementId
import com.kikepb.squadfy.domain.type.ClubMemberId
import java.time.Instant

data class MatchAnnouncementEntryDto(
    val id: MatchAnnouncementEntryId,
    val matchAnnouncementId: MatchAnnouncementId,
    val clubMemberId: ClubMemberId,
    val enrolledAt: Instant
)
