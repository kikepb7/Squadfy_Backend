package com.kikepb.squadfy.domain.exception

class InvalidProfilePictureException(
    override val message: String? = null
): RuntimeException(
    message ?: "Invalid profile picture data"
)