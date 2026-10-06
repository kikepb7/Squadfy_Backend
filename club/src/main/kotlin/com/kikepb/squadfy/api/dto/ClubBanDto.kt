package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.UserId
import java.time.Instant

data class ClubBanDto(
    val clubMemberId: ClubMemberId,
    val userId: UserId,
    val username: String,
    val bannedAt: Instant
)
