package com.kikepb.squadfy.api.controller

import com.kikepb.squadfy.api.dto.UserDto
import com.kikepb.squadfy.api.mappers.toUserDto
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.service.UserProfileService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Me", description = "Authenticated user's own data")
class MeController(
    private val userProfileService: UserProfileService
) {

    @GetMapping
    @Operation(summary = "Profile of the authenticated user, including their email")
    fun getMe(): UserDto =
        userProfileService.getProfile(userId = requestUserId).toUserDto()
}
