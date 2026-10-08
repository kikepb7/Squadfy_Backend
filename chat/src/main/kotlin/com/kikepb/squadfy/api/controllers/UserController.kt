package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.ChatParticipantDto
import com.kikepb.squadfy.api.mappers.toChatParticipantDto
import com.kikepb.squadfy.api.util.requestUserId
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

    @GetMapping("/search")
    @Operation(
        summary = "Search users: usernames containing q (case-insensitive) or the exact email; up to 20, never yourself",
        description = "q needs at least 2 characters (400). Usernames starting with q come first."
    )
    fun searchUsers(@RequestParam("q") q: String): List<ChatParticipantDto> =
        chatParticipantService.searchParticipants(query = q, requestUserId = requestUserId).map { it.toChatParticipantDto() }

    @GetMapping("/{userId}")
    @Operation(summary = "Public profile of a user (use the id from GET /api/v1/me for your own picture)")
    fun getUser(@PathVariable("userId") userId: UserId): ChatParticipantDto =
        chatParticipantService.findChatParticipantById(userId = userId)?.toChatParticipantDto()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
}
