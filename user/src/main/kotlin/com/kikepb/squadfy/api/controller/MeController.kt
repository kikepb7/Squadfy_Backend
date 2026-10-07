package com.kikepb.squadfy.api.controller

import com.kikepb.squadfy.api.config.AuthRateLimits
import com.kikepb.squadfy.api.dto.DeleteAccountRequest
import com.kikepb.squadfy.api.dto.UserDto
import com.kikepb.squadfy.api.mappers.toUserDto
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.service.UserProfileService
import com.kikepb.squadfy.service.account.AccountDeletionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Me", description = "Authenticated user's own data")
class MeController(
    private val userProfileService: UserProfileService,
    private val accountDeletionService: AccountDeletionService,
    private val authRateLimits: AuthRateLimits
) {

    @GetMapping
    @Operation(summary = "Profile of the authenticated user, including their email")
    fun getMe(): UserDto =
        userProfileService.getProfile(userId = requestUserId).toUserDto()

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Delete my account (immediate and irreversible)",
        description = "Confirm with the password. Personal data, messages and devices are deleted; club history stays " +
            "anonymized as 'Usuario eliminado'. Owned clubs pass to the oldest admin or member, or are deleted if empty. 401 if the password is wrong."
    )
    fun deleteMe(@Valid @RequestBody body: DeleteAccountRequest) {
        val userId = requestUserId
        authRateLimits.deleteAccount(userId = userId)
        accountDeletionService.deleteOwnAccount(userId = userId, password = body.password)
    }
}
