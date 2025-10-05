package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.domain.type.ChatMessageId
import com.kikepb.squadfy.service.ChatMessageService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/messages")
class ChatMessageController(
    private val chatMessageService: ChatMessageService
) {

    @DeleteMapping("/{messageId}")
    fun deleteMessage(@PathVariable("messageId") messageId: ChatMessageId) {
        chatMessageService.deleteMessage(messageId = messageId, requestUserId = requestUserId)
    }
}