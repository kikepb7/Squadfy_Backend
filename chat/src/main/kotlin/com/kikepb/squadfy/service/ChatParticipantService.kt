package com.kikepb.squadfy.service

import com.kikepb.squadfy.api.mappers.toChatParticipantDto
import com.kikepb.squadfy.domain.model.ChatParticipantModel
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.mappers.toChatParticipantEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toChatParticipantModel
import com.kikepb.squadfy.infrastructure.database.repositories.ChatParticipantRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class ChatParticipantService(
    private val chatParticipantRepository: ChatParticipantRepository
) {

    fun createChatParticipant(chatParticipant: ChatParticipantModel) {
        chatParticipantRepository.save(
            chatParticipant.toChatParticipantEntity()
        )
    }

    fun findChatParticipantById(userId: UserId): ChatParticipantModel? =
        chatParticipantRepository.findByIdOrNull(userId)?.toChatParticipantModel()

    fun findChatParticipantByEmailOrUsername(query: String): ChatParticipantModel? {
        val normalizedQuery = query.lowercase().trim()
        return chatParticipantRepository.findByEmailOrUsername(query = normalizedQuery)?.toChatParticipantModel()
    }
}