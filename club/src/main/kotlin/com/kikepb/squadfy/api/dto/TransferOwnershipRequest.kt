package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.type.ClubMemberId

data class TransferOwnershipRequest(
    val memberId: ClubMemberId
)
