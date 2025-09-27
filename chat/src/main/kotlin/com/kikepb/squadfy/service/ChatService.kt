package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.exception.ChatParticipantNotFoundException
import com.kikepb.squadfy.domain.exception.InvalidChatSizeException
import com.kikepb.squadfy.domain.model.ChatModel
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.ChatEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toChatModel
import com.kikepb.squadfy.infrastructure.database.repositories.ChatParticipantRepository
import com.kikepb.squadfy.infrastructure.database.repositories.ChatRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChatService(
    private val chatRepository: ChatRepository,
    private val chatParticipantRepository: ChatParticipantRepository
) {

    @Transactional
    fun createChat(creatorId: UserId, otherUserIds: Set<UserId>): ChatModel {
        val otherParticipants = chatParticipantRepository.findByUserIdIn(userIds = otherUserIds)

        val allParticipants = (otherParticipants + creatorId)
        if (allParticipants.size < 2) throw InvalidChatSizeException()

        val creator = chatParticipantRepository.findByIdOrNull(id = creatorId)
            ?: throw ChatParticipantNotFoundException(id = creatorId)

        return chatRepository.save(
            ChatEntity(
                creator = creator,
                participants = setOf(creator) + otherParticipants
            )
        ).toChatModel(lastMessage = null)
    }
}