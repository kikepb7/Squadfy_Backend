package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.ChatParticipantDto
import com.kikepb.squadfy.api.dto.ConfirmProfilePictureRequest
import com.kikepb.squadfy.api.dto.PictureUploadResponse
import com.kikepb.squadfy.api.mappers.toChatParticipantDto
import com.kikepb.squadfy.api.mappers.toResponse
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.service.ChatParticipantService
import com.kikepb.squadfy.service.ProfilePictureService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/participants")
class ChatParticipantController(
    private val chatParticipantService: ChatParticipantService,
    private val profilePictureService: ProfilePictureService
) {

    @GetMapping
    fun getChatParticipantByUsernameOrEmail(@RequestParam(required = false) query: String?): ChatParticipantDto {
        val participant = if (query == null) {
            chatParticipantService.findChatParticipantById(userId = requestUserId)
        } else {
            chatParticipantService.findChatParticipantByEmailOrUsername(query = query)
        }

        return participant?.toChatParticipantDto() ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @PostMapping("/profile-picture-upload")
    fun getProfilePictureUploadUrl(@RequestParam mimeType: String): PictureUploadResponse =
        profilePictureService.generateUploadCredentials(userId = requestUserId, mimeType = mimeType).toResponse()

    @PostMapping("/confirm-profile-picture")
    fun confirmProfilePictureUpload(@Valid @RequestParam body: ConfirmProfilePictureRequest) =
        profilePictureService.confirmProfilePictureUpload(userId = requestUserId, publicUrl = body.publicUrl)

    @DeleteMapping("/delete-profile-picture")
    fun deleteProfilePicture() = profilePictureService.deleteProfilePicture(userId = requestUserId)
}