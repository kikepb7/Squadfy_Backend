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

@Service
class ProfilePictureService(
    private val supabaseStorageService: SupabaseStorageService,
    private val chatParticipantRepository: ChatParticipantRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
    @param:Value("\${supabase.url}") private val supabaseUrl: String,

    ) {

    private val logger = LoggerFactory.getLogger(ProfilePictureService::class.java)

    fun generateUploadCredentials(userId: UserId, mimeType: String): ProfilePictureUploadCredentialsModel {
        return supabaseStorageService.generateSignedUploadUrl(
            userId = userId,
            mimeType = mimeType
        )
    }

    @Transactional
    fun deleteProfilePicture(userId: UserId) {
        val participant = chatParticipantRepository.findByIdOrNull(id =userId)
            ?: throw ChatParticipantNotFoundException(id = userId)

        participant.profilePictureUrl?.let { url ->
            chatParticipantRepository.save(
                participant.apply { profilePictureUrl = null }
            )

            supabaseStorageService.deleteFile(url = url)

            applicationEventPublisher.publishEvent(
                ProfilePictureUpdatedEvent(
                    userId = userId,
                    newUrl = null
                )
            )
        }
    }

    @Transactional
    fun confirmProfilePictureUpload(userId: UserId, publicUrl: String) {
        if (!publicUrl.startsWith(supabaseUrl)) throw InvalidProfilePictureException("Invalid profile picture url")

        val participant = chatParticipantRepository.findByIdOrNull(id =userId)
            ?: throw ChatParticipantNotFoundException(id = userId)

        val oldUrl = participant.profilePictureUrl

        chatParticipantRepository.save(
            participant.apply { profilePictureUrl = publicUrl }
        )

        try {
            oldUrl?.let { supabaseStorageService.deleteFile(url = oldUrl) }
        } catch (e: Exception) {
            logger.warn("Deleting old profile picture for $userId failed", e)
        }

        applicationEventPublisher.publishEvent(
            ProfilePictureUpdatedEvent(
                userId = userId,
                newUrl = publicUrl
            )
        )
    }
}