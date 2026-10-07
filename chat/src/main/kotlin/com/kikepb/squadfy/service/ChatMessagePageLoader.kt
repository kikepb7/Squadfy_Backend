package com.kikepb.squadfy.service

import com.kikepb.squadfy.api.dto.ChatMessageDto
import com.kikepb.squadfy.api.mappers.toChatMessageDto
import com.kikepb.squadfy.domain.type.ChatId
import com.kikepb.squadfy.infrastructure.database.mappers.toChatMessageModel
import com.kikepb.squadfy.infrastructure.database.repositories.ChatMessageRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Loads a page of chat messages. Only the latest default-size page is cached (per chat, evicted
 * when a message is sent or deleted). Callers must check that the requester is a participant:
 * the cached page is shared by everyone in the chat.
 */
@Component
class ChatMessagePageLoader(
    private val chatMessageRepository: ChatMessageRepository
) {

    @Cacheable(
        value = ["messages"],
        key = "#chatId",
        condition = "#before == null && #pageSize == " + DEFAULT_PAGE_SIZE,
        sync = true
    )
    fun loadPage(chatId: ChatId, before: Instant?, pageSize: Int): List<ChatMessageDto> =
        chatMessageRepository
            .findByChatIdBefore(chatId = chatId, before = before ?: Instant.now(), pageable = PageRequest.of(0, pageSize))
            .content
            .asReversed()
            .map { it.toChatMessageModel().toChatMessageDto() }

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
    }
}
