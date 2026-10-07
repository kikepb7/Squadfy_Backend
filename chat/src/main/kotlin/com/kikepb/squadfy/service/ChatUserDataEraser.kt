package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.domain.user.UserDataEraser
import com.kikepb.squadfy.infrastructure.database.repositories.ChatMessageRepository
import com.kikepb.squadfy.infrastructure.database.repositories.ChatParticipantRepository
import com.kikepb.squadfy.infrastructure.database.repositories.ChatRepository
import com.kikepb.squadfy.infrastructure.storage.SupabaseStorageService
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Account deletion in the chat module (spec 010 RN-A3, RN-A7): the user's messages are deleted,
 * they leave every chat (a chat left without participants disappears), the chats they created get
 * another creator, and their profile picture and participant are removed.
 */
@Service
@Order(3)
class ChatUserDataEraser(
    private val chatRepository: ChatRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val chatParticipantRepository: ChatParticipantRepository,
    private val chatService: ChatService,
    private val messageCacheEvictionHelper: MessageCacheEvictionHelper,
    private val storageService: SupabaseStorageService
) : UserDataEraser {

    private val log = LoggerFactory.getLogger(ChatUserDataEraser::class.java)

    @Transactional
    override fun eraseUserData(userId: UserId) {
        val participant = chatParticipantRepository.findByIdOrNull(userId) ?: return

        val chatsWithMessages = chatMessageRepository.findChatIdsWithMessagesFrom(userId = userId)
        chatMessageRepository.deleteAllBySender(userId = userId)

        chatRepository.findAllByUserId(userId = userId).forEach { chat ->
            val chatId = requireNotNull(chat.id)
            if (chat.participants.all { it.userId == userId }) {
                chatMessageRepository.deleteAllByChatIdInBulk(chatId = chatId)
                chatRepository.delete(chat)
            } else {
                chatService.removeParticipantFromChat(chatId = chatId, userId = userId)
            }
        }
        chatRepository.flush()

        chatRepository.findAllCreatedBy(userId = userId).forEach { chat ->
            val newCreator = chat.participants.filter { it.userId != userId }.minByOrNull { it.userId.toString() }
            if (newCreator == null) {
                chatMessageRepository.deleteAllByChatIdInBulk(chatId = requireNotNull(chat.id))
                chatRepository.delete(chat)
            } else {
                chat.creator = newCreator
                chatRepository.save(chat)
            }
        }
        chatRepository.flush()

        chatsWithMessages.forEach { messageCacheEvictionHelper.evictMessagesCache(chatId = it) }
        participant.profilePictureUrl?.let { url ->
            try {
                storageService.deleteFile(url = url)
            } catch (e: Exception) {
                log.warn("[AccountDeletion] Could not delete the profile picture of user={}", userId, e)
            }
        }
        chatParticipantRepository.delete(participant)
        chatParticipantRepository.flush()
    }
}
