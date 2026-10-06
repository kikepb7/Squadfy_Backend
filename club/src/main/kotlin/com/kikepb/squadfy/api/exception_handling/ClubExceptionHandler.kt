package com.kikepb.squadfy.api.exception_handling

import com.kikepb.squadfy.domain.exception.ClubCapacityReachedException
import com.kikepb.squadfy.domain.exception.ClubInviteCodeInvalidException
import com.kikepb.squadfy.domain.exception.ClubMembershipAlreadyExistsException
import com.kikepb.squadfy.domain.exception.ClubMemberBannedException
import com.kikepb.squadfy.domain.exception.ClubMemberNotFoundException
import com.kikepb.squadfy.domain.exception.ClubNotFoundException
import com.kikepb.squadfy.domain.exception.ClubOwnerCannotLeaveException
import com.kikepb.squadfy.domain.exception.InvalidClubOperationException
import com.kikepb.squadfy.domain.exception.ClubParticipantNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ClubExceptionHandler {

    @ExceptionHandler(
        ClubNotFoundException::class,
        ClubParticipantNotFoundException::class,
        ClubMemberNotFoundException::class
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

    @ExceptionHandler(ClubMemberBannedException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun onBanned(e: ClubMemberBannedException) = mapOf(
        "code" to "BANNED_FROM_CLUB",
        "message" to e.message
    )

    @ExceptionHandler(InvalidClubOperationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun onInvalidOperation(e: InvalidClubOperationException) = mapOf(
        "code" to "BAD_REQUEST",
        "message" to e.message
    )

    @ExceptionHandler(
        ClubMembershipAlreadyExistsException::class,
        ClubCapacityReachedException::class,
        ClubOwnerCannotLeaveException::class
    )
    @ResponseStatus(HttpStatus.CONFLICT)
    fun onConflict(e: RuntimeException) = mapOf(
        "code" to "CONFLICT",
        "message" to e.message
    )
}
