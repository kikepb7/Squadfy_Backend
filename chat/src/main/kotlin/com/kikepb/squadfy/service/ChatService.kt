package com.kikepb.squadfy.service

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
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException

@Service
class ChatService(
    private val chatRepository: ChatRepository,
    private val chatParticipantRepository: ChatParticipantRepository,
    private val chatMessageRepository: ChatMessageRepository
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
    }

    private fun lastMessageForChat(chatId: ChatId): ChatMessageModel? {
        return chatMessageRepository
            .findLatestMessagesByChatIds(chatIds = setOf(chatId))
            .firstOrNull()
            ?.toChatMessageModel()
    }
}