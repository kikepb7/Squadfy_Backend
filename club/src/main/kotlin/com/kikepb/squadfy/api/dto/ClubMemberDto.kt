package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.UserId
import java.time.Instant

data class ClubMemberDto(
    val id: ClubMemberId,
    val clubId: ClubId,
    val userId: UserId,
    val username: String,
    /** Picture to show: [clubPictureUrl] if set, otherwise [profilePictureUrl]. */
    val pictureUrl: String?,
    val profilePictureUrl: String?,
    val clubPictureUrl: String?,
    val shirtNumber: Int?,
    val position: String?,
    val role: ClubMemberRole,
    val createdAt: Instant,
    val updatedAt: Instant
)
