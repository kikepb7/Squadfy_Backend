package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.UserId
import java.time.Instant

data class ClubDto(
    val id: ClubId,
    val name: String,
    val description: String?,
    val clubLogoUrl: String?,
    val ownerId: UserId,
    val invitationCode: String,
    val maxMembers: Int?,
    val membersCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)
