package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.UserId
import java.time.Instant

data class ClubBanModel(
    val clubMemberId: ClubMemberId,
    val userId: UserId,
    val username: String,
    val bannedAt: Instant
)
