package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.ChatParticipantDto
import com.kikepb.squadfy.api.mappers.toChatParticipantDto
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.service.ChatParticipantService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/chat/participants")
class ChatParticipantController(
    private val chatParticipantService: ChatParticipantService
) {

    @GetMapping
    fun getChatParticipantByUsernameOrEmail(@RequestParam(required = false) query: String?): ChatParticipantDto {
        val participant = if (query == null) {
            chatParticipantService.findChatParticipantById(userId = requestUserId)
        } else {
            chatParticipantService.findChatParticipantByEmailOrUsername(query = query)
        }

        return participant?.toChatParticipantDto() ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }
}