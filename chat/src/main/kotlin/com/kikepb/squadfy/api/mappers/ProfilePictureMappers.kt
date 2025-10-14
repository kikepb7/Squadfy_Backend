package com.kikepb.squadfy.api.mappers

import com.kikepb.squadfy.api.dto.PictureUploadResponse
import com.kikepb.squadfy.domain.model.ProfilePictureUploadCredentialsModel

fun ProfilePictureUploadCredentialsModel.toResponse(): PictureUploadResponse {
    return PictureUploadResponse(
        uploadUrl = uploadUrl,
        publicUrl = publicUrl,
        headers = headers,
        expiresAt = expiresAt
    )
}