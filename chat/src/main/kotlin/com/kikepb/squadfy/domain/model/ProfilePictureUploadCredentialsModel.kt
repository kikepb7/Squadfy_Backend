package com.kikepb.squadfy.domain.model

import java.time.Instant

data class ProfilePictureUploadCredentialsModel(
    val uploadUrl: String,
    val publicUrl: String,
    val headers: Map<String, String>,
    val expiresAt: Instant
)
