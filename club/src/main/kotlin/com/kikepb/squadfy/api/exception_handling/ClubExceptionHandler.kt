package com.kikepb.squadfy.api.exception_handling

import com.kikepb.squadfy.domain.exception.ClubCapacityReachedException
import com.kikepb.squadfy.domain.exception.ClubInviteCodeInvalidException
import com.kikepb.squadfy.domain.exception.ClubMembershipAlreadyExistsException
import com.kikepb.squadfy.domain.exception.ClubNotFoundException
import com.kikepb.squadfy.domain.exception.ClubParticipantNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ClubExceptionHandler {

    @ExceptionHandler(
        ClubNotFoundException::class,
        ClubParticipantNotFoundException::class
    )
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun onNotFound(e: RuntimeException) = mapOf(
        "code" to "NOT_FOUND",
        "message" to e.message
    )

    @ExceptionHandler(ClubInviteCodeInvalidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun onInvalidInvitationCode(e: ClubInviteCodeInvalidException) = mapOf(
        "code" to "INVALID_INVITATION_CODE",
        "message" to e.message
    )

    @ExceptionHandler(
        ClubMembershipAlreadyExistsException::class,
        ClubCapacityReachedException::class
    )
    @ResponseStatus(HttpStatus.CONFLICT)
    fun onConflict(e: RuntimeException) = mapOf(
        "code" to "CONFLICT",
        "message" to e.message
    )
}
