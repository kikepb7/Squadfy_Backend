package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.ConfirmProfilePictureRequest
import com.kikepb.squadfy.api.dto.PictureUploadResponse
import com.kikepb.squadfy.api.mappers.toResponse
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.service.ProfilePictureService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/me/profile-picture")
@Tag(name = "Me", description = "Authenticated user's own data")
class ProfilePictureController(
    private val profilePictureService: ProfilePictureService
) {

    @PostMapping("/upload-url")
    @Operation(summary = "Signed URL to upload a new profile picture directly to storage")
    fun getUploadUrl(@RequestParam("mimeType") mimeType: String): PictureUploadResponse =
        profilePictureService.generateUploadCredentials(userId = requestUserId, mimeType = mimeType).toResponse()

    @PutMapping
    @Operation(summary = "Confirm the uploaded picture as my profile picture")
    fun confirmUpload(@Valid @RequestBody body: ConfirmProfilePictureRequest) =
        profilePictureService.confirmProfilePictureUpload(userId = requestUserId, publicUrl = body.publicUrl)

    @DeleteMapping
    @Operation(summary = "Remove my profile picture")
    fun deleteProfilePicture() = profilePictureService.deleteProfilePicture(userId = requestUserId)
}
