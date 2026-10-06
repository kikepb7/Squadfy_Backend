package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.ChatParticipantDto
import com.kikepb.squadfy.api.mappers.toChatParticipantDto
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.service.ChatParticipantService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** Public user profiles (username and picture); they live in the chat module's participant directory. */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Public profiles: username and profile picture, never the email")
class UserController(
    private val chatParticipantService: ChatParticipantService
) {

    @GetMapping
    @Operation(summary = "Find a user by exact username or email")
    fun findUser(@RequestParam("query") query: String): ChatParticipantDto =
        chatParticipantService.findChatParticipantByEmailOrUsername(query = query)?.toChatParticipantDto()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

    @GetMapping("/{userId}")
    @Operation(summary = "Public profile of a user (use the id from GET /api/v1/me for your own picture)")
    fun getUser(@PathVariable("userId") userId: UserId): ChatParticipantDto =
        chatParticipantService.findChatParticipantById(userId = userId)?.toChatParticipantDto()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
}
