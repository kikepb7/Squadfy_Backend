package com.kikepb.squadfy.service

import com.kikepb.squadfy.api.dto.ChatMessageDto
import com.kikepb.squadfy.api.mappers.toChatMessageDto
import com.kikepb.squadfy.domain.event.ChatCreatedEvent
import com.kikepb.squadfy.domain.event.ChatParticipantLeftEvent
import com.kikepb.squadfy.domain.event.ChatParticipantsJoinedEvent
import com.kikepb.squadfy.domain.exception.ChatNotFoundException
import com.kikepb.squadfy.domain.exception.ChatParticipantNotFoundException
import com.kikepb.squadfy.domain.exception.ForbiddenException
import com.kikepb.squadfy.domain.exception.InvalidChatSizeException
import com.kikepb.squadfy.domain.model.ChatMessageModel
import com.kikepb.squadfy.domain.model.ChatModel
import com.kikepb.squadfy.domain.type.ChatId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.ChatEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toChatMessageModel
import com.kikepb.squadfy.infrastructure.database.mappers.toChatModel
import com.kikepb.squadfy.infrastructure.database.repositories.ChatMessageRepository
import com.kikepb.squadfy.infrastructure.database.repositories.ChatParticipantRepository
import com.kikepb.squadfy.infrastructure.database.repositories.ChatRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ChatService(
    private val chatRepository: ChatRepository,
    private val chatParticipantRepository: ChatParticipantRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val chatMessagePageLoader: ChatMessagePageLoader,
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    @Transactional
    fun createChat(creatorId: UserId, otherUserIds: Set<UserId>): ChatModel {
        val otherParticipants = chatParticipantRepository.findByUserIdIn(userIds = otherUserIds)

        val allParticipants = (otherParticipants + creatorId)
        if (allParticipants.size < 2) throw InvalidChatSizeException()

        val creator = chatParticipantRepository.findByIdOrNull(id = creatorId)
            ?: throw ChatParticipantNotFoundException(id = creatorId)

        return chatRepository.saveAndFlush(
            ChatEntity(
                creator = creator,
                participants = setOf(creator) + otherParticipants
            )
        ).toChatModel(lastMessage = null).also { entity ->
            applicationEventPublisher.publishEvent(
                ChatCreatedEvent(
                    chatId = entity.id,
                    participantIds = entity.participants.map { it.userId }
                )
            )
        }
    }

    @Transactional
    fun addParticipantsToChat(requestUserId: UserId, chatId: ChatId, userIds: Set<UserId>): ChatModel {
        val chat = chatRepository.findByIdOrNull(id = chatId)
            ?: throw ChatNotFoundException()

        val isRequestingUserInChat = chat.participants.any {
            it.userId == requestUserId
        }
        if (!isRequestingUserInChat) throw ForbiddenException()

        val users = userIds.map { userId ->
            chatParticipantRepository.findByIdOrNull(id = userId)
                ?: throw ChatParticipantNotFoundException(id = userId)
        }

        val lastMessage = lastMessageForChat(chatId = chatId)

        val updatedChat = chatRepository.save(
            chat.apply {
                this.participants = chat.participants + users
            }
        ).toChatModel(lastMessage = lastMessage)

        applicationEventPublisher.publishEvent(
            ChatParticipantsJoinedEvent(
                chatId = chatId,
                userIds = userIds
            )
        )

        return updatedChat
    }

    @Transactional
    fun removeParticipantFromChat(chatId: ChatId, userId: UserId) {
        val chat = chatRepository.findByIdOrNull(id = chatId)
            ?: throw ChatNotFoundException()

        val participant = chat.participants.find { it.userId == userId }
            ?: throw ChatParticipantNotFoundException(id = userId)

        val newParticipantsSize = chat.participants.size - 1

        if (newParticipantsSize == 0) {
            chatRepository.deleteById(chatId)
            return
        }

        chatRepository.save(
            chat.apply {
                this.participants = chat.participants - participant
            }
        )

        applicationEventPublisher.publishEvent(
            ChatParticipantLeftEvent(
                chatId = chatId,
                userId = userId
            )
        )
    }

    /**
     * Messages before [before], oldest first; only for participants of the chat. The permission
     * check runs outside the cache: the cached page is shared by every participant of the chat.
     */
    fun getChatMessages(chatId: ChatId, requestUserId: UserId, before: Instant?, pageSize: Int): List<ChatMessageDto> {
        chatRepository.findChatById(id = chatId, userId = requestUserId) ?: throw ForbiddenException()
        return chatMessagePageLoader.loadPage(chatId = chatId, before = before, pageSize = pageSize.coerceIn(1, MAX_PAGE_SIZE))
    }

    private fun lastMessageForChat(chatId: ChatId): ChatMessageModel? {
        return chatMessageRepository
            .findLatestMessagesByChatIds(chatIds = setOf(chatId))
            .firstOrNull()
            ?.toChatMessageModel()
    }

    fun getChatById(chatId: ChatId, requestUserId: UserId): ChatModel? {
        return chatRepository.findChatById(id = chatId, userId = requestUserId)
            ?.toChatModel(lastMessage = lastMessageForChat(chatId = chatId))
    }

    fun findChatByUser(userId: UserId): List<ChatModel> {
        val chatEntities = chatRepository.findAllByUserId(userId = userId)
        val chatIds = chatEntities.mapNotNull { it.id }
        val latestMessages = chatMessageRepository
            .findLatestMessagesByChatIds(chatIds = chatIds.toSet())
            .associateBy { it.chatId }

        return chatEntities
            .map { it.toChatModel(lastMessage = latestMessages[it.id]?.toChatMessageModel()) }
            .sortedBy { it.lastActivityAt }
    }

    private companion object {
        const val MAX_PAGE_SIZE = 100
    }
}
