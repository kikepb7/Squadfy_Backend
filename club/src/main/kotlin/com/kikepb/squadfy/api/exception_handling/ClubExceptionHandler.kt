package com.kikepb.squadfy.api.exception_handling

import com.kikepb.squadfy.domain.exception.ClubCapacityReachedException
import com.kikepb.squadfy.domain.exception.ClubInviteCodeInvalidException
import com.kikepb.squadfy.domain.exception.ClubMatchNotFoundException
import com.kikepb.squadfy.domain.exception.ClubMembershipAlreadyExistsException
import com.kikepb.squadfy.domain.exception.ClubNotFoundException
import com.kikepb.squadfy.domain.exception.ClubParticipantNotFoundException
import com.kikepb.squadfy.domain.exception.InvalidTeamGenerationRequestException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ClubExceptionHandler {

    @ExceptionHandler(
        ClubNotFoundException::class,
        ClubMatchNotFoundException::class,
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

    @ExceptionHandler(InvalidTeamGenerationRequestException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun onInvalidTeamGenerationRequest(e: InvalidTeamGenerationRequestException) = mapOf(
        "code" to "INVALID_TEAM_GENERATION_REQUEST",
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

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun onValidationException(e: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val errors = e.bindingResult.allErrors.map {
            it.defaultMessage ?: "Invalid value"
        }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                mapOf(
                    "code" to "VALIDATION_ERROR",
                    "errors" to errors
                )
            )
    }
}
