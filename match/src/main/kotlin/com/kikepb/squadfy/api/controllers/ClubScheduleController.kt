package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.ClubMatchScheduleDto
import com.kikepb.squadfy.api.dto.CreateClubMatchScheduleRequest
import com.kikepb.squadfy.api.dto.CreateScheduleExceptionRequest
import com.kikepb.squadfy.api.dto.ScheduleExceptionDto
import com.kikepb.squadfy.api.dto.UpdateClubMatchScheduleRequest
import com.kikepb.squadfy.api.mappers.toClubMatchScheduleDto
import com.kikepb.squadfy.api.mappers.toScheduleExceptionDto
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.domain.model.DeadlineRule
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.service.ClubMatchScheduleService
import com.kikepb.squadfy.service.ScheduleExceptionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/clubs/{clubId}/schedule")
@Tag(name = "Schedule", description = "Weekly match schedule of a club (one per club)")
class ClubScheduleController(
    private val clubMatchScheduleService: ClubMatchScheduleService,
    private val scheduleExceptionService: ScheduleExceptionService
) {

    @GetMapping
    @Operation(summary = "Weekly schedule of the club (members only)")
    fun getSchedule(@PathVariable("clubId") clubId: ClubId): ClubMatchScheduleDto =
        clubMatchScheduleService
            .getScheduleByClub(clubId = clubId, userId = requestUserId)
            .toClubMatchScheduleDto()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create the weekly schedule (managers only); the first match is planned immediately")
    fun createSchedule(
        @PathVariable("clubId") clubId: ClubId,
        @Valid @RequestBody body: CreateClubMatchScheduleRequest
    ): ClubMatchScheduleDto =
        clubMatchScheduleService.createSchedule(
            clubId = clubId,
            userId = requestUserId,
            matchDayOfWeek = body.matchDayOfWeek,
            matchTime = body.matchTime,
            timeZone = body.timeZone,
            format = body.format,
            matchDurationMinutes = body.matchDurationMinutes,
            close = DeadlineRule(daysBefore = body.closeDaysBefore, time = body.closeTime),
            draw = if (body.drawDaysBefore == null && body.drawTime == null) null
            else DeadlineRule(daysBefore = body.drawDaysBefore ?: body.closeDaysBefore, time = body.drawTime ?: body.closeTime)
        ).toClubMatchScheduleDto()

    @PatchMapping
    @Operation(summary = "Update the weekly schedule (managers only); applies from the next planned match")
    fun updateSchedule(
        @PathVariable("clubId") clubId: ClubId,
        @Valid @RequestBody body: UpdateClubMatchScheduleRequest
    ): ClubMatchScheduleDto =
        clubMatchScheduleService.updateSchedule(
            clubId = clubId,
            userId = requestUserId,
            matchDayOfWeek = body.matchDayOfWeek,
            matchTime = body.matchTime,
            timeZone = body.timeZone,
            format = body.format,
            isActive = body.isActive,
            matchDurationMinutes = body.matchDurationMinutes,
            closeDaysBefore = body.closeDaysBefore,
            closeTime = body.closeTime,
            drawDaysBefore = body.drawDaysBefore,
            drawTime = body.drawTime
        ).toClubMatchScheduleDto()

    @GetMapping("/exceptions")
    @Operation(summary = "Exceptions of the weekly schedule: cancelled or moved weeks (members only)")
    fun getExceptions(@PathVariable("clubId") clubId: ClubId): List<ScheduleExceptionDto> =
        scheduleExceptionService
            .getExceptions(clubId = clubId, userId = requestUserId)
            .map { it.toScheduleExceptionDto() }

    @PostMapping("/exceptions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Cancel or move the match of one week (managers only)",
        description = "date is the club's match day of that week. CANCELLED: no match that week (an already planned match is cancelled). " +
            "RESCHEDULED: the match is played at newScheduledAt (an already planned match is moved keeping its enrollments)."
    )
    fun createException(
        @PathVariable("clubId") clubId: ClubId,
        @Valid @RequestBody body: CreateScheduleExceptionRequest
    ): ScheduleExceptionDto =
        scheduleExceptionService.createException(
            clubId = clubId,
            userId = requestUserId,
            date = body.date,
            type = body.type,
            newScheduledAt = body.newScheduledAt,
            reason = body.reason?.trim()?.ifBlank { null }
        ).toScheduleExceptionDto()

    @DeleteMapping("/exceptions/{exceptionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Undo an exception while its date is in the future (managers only)")
    fun deleteException(
        @PathVariable("clubId") clubId: ClubId,
        @PathVariable("exceptionId") exceptionId: UUID
    ) {
        scheduleExceptionService.deleteException(clubId = clubId, userId = requestUserId, exceptionId = exceptionId)
    }
}
