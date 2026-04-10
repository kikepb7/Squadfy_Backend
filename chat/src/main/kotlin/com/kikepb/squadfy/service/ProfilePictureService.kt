package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.event.ProfilePictureUpdatedEvent
import com.kikepb.squadfy.domain.exception.ChatParticipantNotFoundException
import com.kikepb.squadfy.domain.exception.InvalidProfilePictureException
import com.kikepb.squadfy.domain.model.ProfilePictureUploadCredentialsModel
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.repositories.ChatParticipantRepository
import com.kikepb.squadfy.infrastructure.storage.SupabaseStorageService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class ProfilePictureService(
    private val supabaseStorageService: SupabaseStorageService,
    private val chatParticipantRepository: ChatParticipantRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
    @param:Value("\${supabase.project-url}") private val supabaseProjectUrl: String
) {
    private val logger = LoggerFactory.getLogger(ProfilePictureService::class.java)

    companion object {
        private const val BUCKET = "profile-pictures"
        private const val SIGNED_URL_EXPIRY_SECONDS = 300
    }

    fun generateUploadCredentials(userId: UserId, mimeType: String): ProfilePictureUploadCredentialsModel {
        val extension = SupabaseStorageService.ALLOWED_IMAGE_MIME_TYPES[mimeType]
            ?: throw InvalidProfilePictureException("Invalid mime type $mimeType")

        val fileName = "user_${userId}_${UUID.randomUUID()}.$extension"
        val storagePath = "$BUCKET/$fileName"

        return ProfilePictureUploadCredentialsModel(
            uploadUrl = supabaseStorageService.createSignedUploadUrl(
                storagePath = storagePath,
                expiresInSeconds = SIGNED_URL_EXPIRY_SECONDS
            ),
            publicUrl = supabaseStorageService.publicUrl(BUCKET, fileName),
            headers = mapOf("Content-Type" to mimeType),
            expiresAt = Instant.now().plusSeconds(SIGNED_URL_EXPIRY_SECONDS.toLong())
        )
    }

    @Transactional
    fun deleteProfilePicture(userId: UserId) {
        val participant = chatParticipantRepository.findByIdOrNull(id = userId)
            ?: throw ChatParticipantNotFoundException(id = userId)

        participant.profilePictureUrl?.let { url ->
            chatParticipantRepository.save(
                participant.apply { profilePictureUrl = null }
            )

            supabaseStorageService.deleteFile(url = url)

            applicationEventPublisher.publishEvent(
                ProfilePictureUpdatedEvent(userId = userId, newUrl = null)
            )
        }
    }

    @Transactional
    fun confirmProfilePictureUpload(userId: UserId, publicUrl: String) {
        if (!publicUrl.startsWith(supabaseProjectUrl)) throw InvalidProfilePictureException("Invalid profile picture url")

        val participant = chatParticipantRepository.findByIdOrNull(id = userId)
            ?: throw ChatParticipantNotFoundException(id = userId)

        val oldUrl = participant.profilePictureUrl

        chatParticipantRepository.save(
            participant.apply { profilePictureUrl = publicUrl }
        )

        try {
            oldUrl?.let { supabaseStorageService.deleteFile(url = it) }
        } catch (e: Exception) {
            logger.warn("Deleting old profile picture for $userId failed", e)
        }

        applicationEventPublisher.publishEvent(
            ProfilePictureUpdatedEvent(userId = userId, newUrl = publicUrl)
        )
    }
}
