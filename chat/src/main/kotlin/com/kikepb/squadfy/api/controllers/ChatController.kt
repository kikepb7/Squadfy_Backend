package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.AddParticipantToChatDto
import com.kikepb.squadfy.api.dto.ChatDto
import com.kikepb.squadfy.api.dto.ChatMessageDto
import com.kikepb.squadfy.api.dto.CreateChatRequest
import com.kikepb.squadfy.api.mappers.toChatDto
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.domain.type.ChatId
import com.kikepb.squadfy.service.ChatService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/chat")
class ChatController(
    private val chatService: ChatService
) {

    companion object {
        private const val DEFAULT_PAGE_SIZE = 20
    }

    @GetMapping("/{chatId}/messages")
    fun getMessagesForChat(
        @PathVariable("chatId") chatId: ChatId,
        @RequestParam("before", required = false) before: Instant? = null,
        @RequestParam("pageSize", required = false) pageSize: Int = DEFAULT_PAGE_SIZE
    ): List<ChatMessageDto> {
        return chatService.getChatMessage(chatId = chatId, before = before, pageSize = pageSize)
    }

    @PostMapping("/create-chat")
    fun createChat(@Valid @RequestBody body: CreateChatRequest): ChatDto {
        return chatService.createChat(
            creatorId = requestUserId,
            otherUserIds = body.otherUserIds.toSet()
        ).toChatDto()
    }

    @PostMapping("/{chatId}/add")
    fun addChatParticipants(@PathVariable chatId: ChatId, @Valid @RequestBody body: AddParticipantToChatDto): ChatDto {
        return chatService.addParticipantsToChat(
            requestUserId = requestUserId,
            chatId = chatId,
            userIds = body.userIds.toSet()
        ).toChatDto()
    }

    @DeleteMapping("/{chatId}/leave")
    fun leaveChat(@PathVariable chatId: ChatId) {
        chatService.removeParticipantFromChat(
            chatId = chatId,
            userId = requestUserId
        )
    }
}