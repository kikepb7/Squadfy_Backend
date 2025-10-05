package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.AddParticipantToChatDto
import com.kikepb.squadfy.api.dto.ChatDto
import com.kikepb.squadfy.api.dto.CreateChatRequest
import com.kikepb.squadfy.api.mappers.toChatDto
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.domain.type.ChatId
import com.kikepb.squadfy.service.ChatService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/chat")
class ChatController(
    private val chatService: ChatService
) {

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