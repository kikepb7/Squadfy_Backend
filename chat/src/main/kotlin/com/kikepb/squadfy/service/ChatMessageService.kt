package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.exception.ChatNotFoundException
import com.kikepb.squadfy.domain.exception.ChatParticipantNotFoundException
import com.kikepb.squadfy.domain.exception.ForbiddenException
import com.kikepb.squadfy.domain.exception.MessageNotFoundException
import com.kikepb.squadfy.domain.model.ChatMessageModel
import com.kikepb.squadfy.domain.type.ChatId
import com.kikepb.squadfy.domain.type.ChatMessageId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.ChatMessageEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toChatMessageModel
import com.kikepb.squadfy.infrastructure.database.repositories.ChatMessageRepository
import com.kikepb.squadfy.infrastructure.database.repositories.ChatParticipantRepository
import com.kikepb.squadfy.infrastructure.database.repositories.ChatRepository
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class ChatMessageService(
    private val chatRepository: ChatRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val chatParticipantRepository: ChatParticipantRepository
) {

    @Transactional
    fun sendMessage(chatId: ChatId, senderId: UserId, content: String, messageId: ChatMessageId? = null): ChatMessageModel {
        val chat = chatRepository.findChatById(id = chatId, userId = senderId)
            ?: throw ChatNotFoundException()
        val sender = chatParticipantRepository.findByIdOrNull(id = senderId)
            ?: throw ChatParticipantNotFoundException(id = senderId)

        val savedMessage = chatMessageRepository.save(
            ChatMessageEntity(
                id = messageId,
                content = content.trim(),
                chatId = chatId,
                chat = chat,
                sender = sender
            )
        )

        return savedMessage.toChatMessageModel()
    }

    @Transactional
    fun deleteMessage(messageId: ChatMessageId, requestUserId: UserId) {
        val message = chatMessageRepository.findByIdOrNull(id = messageId)
            ?: throw MessageNotFoundException(id = messageId)

        if (message.sender.userId != requestUserId) throw ForbiddenException()

        chatMessageRepository.delete(message)
    }
}