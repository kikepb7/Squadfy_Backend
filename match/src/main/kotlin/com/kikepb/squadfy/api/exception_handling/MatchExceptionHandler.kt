package com.kikepb.squadfy.api.exception_handling

import com.kikepb.squadfy.domain.exception.MatchAnnouncementAlreadyEnrolledException
import com.kikepb.squadfy.domain.exception.MatchAnnouncementClosedException
import com.kikepb.squadfy.domain.exception.MatchAnnouncementEntryNotFoundException
import com.kikepb.squadfy.domain.exception.MatchAnnouncementFullException
import com.kikepb.squadfy.domain.exception.MatchAnnouncementNotFoundException
import com.kikepb.squadfy.domain.exception.ClubMatchScheduleAlreadyExistsException
import com.kikepb.squadfy.domain.exception.ClubMatchScheduleNotFoundException
import com.kikepb.squadfy.domain.exception.InvalidTeamGenerationRequestException
import com.kikepb.squadfy.domain.exception.MatchEventNotFoundException
import com.kikepb.squadfy.domain.exception.MatchNotFoundException
import com.kikepb.squadfy.domain.exception.NotClubMemberException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class MatchExceptionHandler {

    @ExceptionHandler(
        MatchNotFoundException::class,
        MatchEventNotFoundException::class,
        MatchAnnouncementNotFoundException::class,
        MatchAnnouncementEntryNotFoundException::class,
        ClubMatchScheduleNotFoundException::class
    )
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun onNotFound(e: RuntimeException) = mapOf(
        "code" to "NOT_FOUND",
        "message" to e.message
    )

    @ExceptionHandler(
        MatchAnnouncementClosedException::class,
        NotClubMemberException::class,
        InvalidTeamGenerationRequestException::class
    )
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun onBadRequest(e: RuntimeException) = mapOf(
        "code" to "BAD_REQUEST",
        "message" to e.message
    )

    @ExceptionHandler(
        MatchAnnouncementFullException::class,
        MatchAnnouncementAlreadyEnrolledException::class,
        ClubMatchScheduleAlreadyExistsException::class
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
