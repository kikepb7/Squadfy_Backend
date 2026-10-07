package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.ClubMatchScheduleDto
import com.kikepb.squadfy.api.dto.CreateClubMatchScheduleRequest
import com.kikepb.squadfy.api.dto.UpdateClubMatchScheduleRequest
import com.kikepb.squadfy.api.mappers.toClubMatchScheduleDto
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.service.ClubMatchScheduleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/clubs/{clubId}/schedule")
@Tag(name = "Schedule", description = "Weekly match schedule of a club (one per club)")
class ClubScheduleController(
    private val clubMatchScheduleService: ClubMatchScheduleService
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
            matchDurationMinutes = body.matchDurationMinutes
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
            matchDurationMinutes = body.matchDurationMinutes
        ).toClubMatchScheduleDto()
}
