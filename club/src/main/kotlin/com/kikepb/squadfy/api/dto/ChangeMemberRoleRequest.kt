package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.model.ClubMemberModel.ClubMemberRole

data class ChangeMemberRoleRequest(
    /** ADMIN, CAPTAIN or PLAYER; OWNER changes only through transfer-ownership. */
    val role: ClubMemberRole
)
