package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.type.UserId
import java.util.UUID

typealias ClubId = UUID

data class ClubModel(
    val id: ClubId,
    val name: String,
    val description: String?,
    val logoUrl: String?,
    val ownerId: UserId
)
