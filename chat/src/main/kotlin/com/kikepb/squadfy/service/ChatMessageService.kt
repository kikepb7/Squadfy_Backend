package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.event.MessageDeletedEvent
import com.kikepb.squadfy.domain.events.chat.ChatEvent
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
import com.kikepb.squadfy.infrastructure.message_queue.EventPublisher
import jakarta.transaction.Transactional
import org.springframework.cache.annotation.CacheEvict
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ChatMessageService(
    private val chatRepository: ChatRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val chatParticipantRepository: ChatParticipantRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val eventPublisher: EventPublisher,
    private val messageCacheEvictionHelper: MessageCacheEvictionHelper
) {

    @Transactional
    @CacheEvict(
        value = ["messages"],
        key = "#chatId",
    )
    fun sendMessage(chatId: ChatId, senderId: UserId, content: String, messageId: ChatMessageId? = null): ChatMessageModel {
        val chat = chatRepository.findChatById(id = chatId, userId = senderId)
            ?: throw ChatNotFoundException()
        val sender = chatParticipantRepository.findByIdOrNull(id = senderId)
            ?: throw ChatParticipantNotFoundException(id = senderId)

        val savedMessage = chatMessageRepository.saveAndFlush(
            ChatMessageEntity(
                id = messageId ?: UUID.randomUUID(),
                content = content.trim(),
                chatId = chatId,
                chat = chat,
                sender = sender
            )
        )

        eventPublisher.publish(
            event = ChatEvent.NewMessage(
                senderId = sender.userId,
                senderUsername = sender.username,
                recipientIds = chat.participants.map { it.userId }.toSet(),
                chatId = chatId,
                message = savedMessage.content
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

        applicationEventPublisher.publishEvent(
            MessageDeletedEvent(
                chatId = message.chatId,
                messageId = messageId
            )
        )

        messageCacheEvictionHelper.evictMessagesCache(chatId = message.chatId)
    }
}