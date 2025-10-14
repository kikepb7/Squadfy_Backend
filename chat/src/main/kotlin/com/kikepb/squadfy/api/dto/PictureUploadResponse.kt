package com.kikepb.squadfy.api.dto

import java.time.Instant

data class PictureUploadResponse(
    val uploadUrl: String,
    val publicUrl: String,
    val headers: Map<String, String>,
    val expiresAt: Instant
)
