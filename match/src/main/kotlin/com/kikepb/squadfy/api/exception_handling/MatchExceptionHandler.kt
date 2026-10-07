package com.kikepb.squadfy.api.exception_handling

import com.kikepb.squadfy.domain.exception.ClubMatchScheduleAlreadyExistsException
import com.kikepb.squadfy.domain.exception.ClubMatchScheduleNotFoundException
import com.kikepb.squadfy.domain.exception.InvalidClubMatchScheduleException
import com.kikepb.squadfy.domain.exception.InvalidMatchEventException
import com.kikepb.squadfy.domain.exception.InvalidMatchStateException
import com.kikepb.squadfy.domain.exception.InvalidPlayerMinutesException
import com.kikepb.squadfy.domain.exception.InvalidTeamGenerationRequestException
import com.kikepb.squadfy.domain.exception.MatchAnnouncementAlreadyEnrolledException
import com.kikepb.squadfy.domain.exception.MatchAnnouncementClosedException
import com.kikepb.squadfy.domain.exception.MatchAnnouncementEntryNotFoundException
import com.kikepb.squadfy.domain.exception.MatchAnnouncementNotFoundException
import com.kikepb.squadfy.domain.exception.MatchEventNotFoundException
import com.kikepb.squadfy.domain.exception.MatchNotFoundException
import com.kikepb.squadfy.domain.exception.NotClubMemberException
import com.kikepb.squadfy.domain.exception.GuestNotFoundException
import com.kikepb.squadfy.domain.exception.InvalidMatchScoreException
import com.kikepb.squadfy.domain.exception.InvalidMemberAbsenceException
import com.kikepb.squadfy.domain.exception.InvalidScheduleExceptionException
import com.kikepb.squadfy.domain.exception.MemberAbsenceNotFoundException
import com.kikepb.squadfy.domain.exception.ScheduleExceptionAlreadyExistsException
import com.kikepb.squadfy.domain.exception.ScheduleExceptionNotFoundException
import com.kikepb.squadfy.domain.exception.TooManyGuestsException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
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
        ClubMatchScheduleNotFoundException::class,
        GuestNotFoundException::class,
        ScheduleExceptionNotFoundException::class,
        MemberAbsenceNotFoundException::class
    )
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun onNotFound(e: RuntimeException) = mapOf(
        "code" to "NOT_FOUND",
        "message" to e.message
    )

    @ExceptionHandler(NotClubMemberException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun onNotClubMember(e: NotClubMemberException) = mapOf(
        "code" to "NOT_CLUB_MEMBER",
        "message" to e.message
    )

    @ExceptionHandler(
        MatchAnnouncementClosedException::class,
        InvalidTeamGenerationRequestException::class,
        InvalidMatchEventException::class,
        InvalidClubMatchScheduleException::class,
        InvalidPlayerMinutesException::class,
        InvalidMatchScoreException::class,
        InvalidScheduleExceptionException::class,
        InvalidMemberAbsenceException::class
    )
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun onBadRequest(e: RuntimeException) = mapOf(
        "code" to "BAD_REQUEST",
        "message" to e.message
    )

    @ExceptionHandler(
        MatchAnnouncementAlreadyEnrolledException::class,
        ClubMatchScheduleAlreadyExistsException::class,
        InvalidMatchStateException::class,
        TooManyGuestsException::class,
        ScheduleExceptionAlreadyExistsException::class
    )
    @ResponseStatus(HttpStatus.CONFLICT)
    fun onConflict(e: RuntimeException) = mapOf(
        "code" to "CONFLICT",
        "message" to e.message
    )

    @ExceptionHandler(DataIntegrityViolationException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun onDataIntegrityViolation(e: DataIntegrityViolationException) = mapOf(
        "code" to "CONFLICT",
        "message" to "The operation conflicts with existing data"
    )
}
