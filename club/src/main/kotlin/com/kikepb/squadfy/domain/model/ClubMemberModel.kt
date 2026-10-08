package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.UserId
import java.time.Instant

data class ClubMemberModel(
    val id: ClubMemberId,
    val clubId: ClubId,
    val userId: UserId,
    val username: String,
    val email: String,
    val profilePictureUrl: String?,
    /** Picture of the member in this club (spec 012 RN-C1). */
    val clubPictureUrl: String? = null,
    val shirtNumber: Int?,
    val position: String?,
    val role: ClubMemberRole,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    /** The picture to show: the club one, otherwise the profile one (RN-C2). */
    val pictureUrl: String? get() = clubPictureUrl ?: profilePictureUrl

    enum class ClubMemberRole {
        OWNER,
        ADMIN,
        CAPTAIN,
        PLAYER
    }
}
