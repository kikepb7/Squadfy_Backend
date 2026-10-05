package com.kikepb.squadfy.api.controllers

import com.kikepb.squadfy.api.dto.ClubMatchScheduleDto
import com.kikepb.squadfy.api.dto.CreateClubMatchScheduleRequest
import com.kikepb.squadfy.api.dto.UpdateClubMatchScheduleRequest
import com.kikepb.squadfy.api.mappers.toClubMatchScheduleDto
import com.kikepb.squadfy.api.util.requestUserId
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMatchScheduleId
import com.kikepb.squadfy.service.ClubMatchScheduleService
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
@RequestMapping("/api/match-schedules")
class ClubMatchScheduleController(
    private val clubMatchScheduleService: ClubMatchScheduleService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createSchedule(@Valid @RequestBody body: CreateClubMatchScheduleRequest): ClubMatchScheduleDto =
        clubMatchScheduleService.createSchedule(
            clubId = body.clubId,
            userId = requestUserId,
            matchDayOfWeek = body.matchDayOfWeek,
            matchTime = body.matchTime,
            timeZone = body.timeZone,
            format = body.format
        ).toClubMatchScheduleDto()

    @GetMapping("/club/{clubId}")
    fun getScheduleByClub(@PathVariable("clubId") clubId: ClubId): ClubMatchScheduleDto =
        clubMatchScheduleService
            .getScheduleByClub(clubId = clubId, userId = requestUserId)
            .toClubMatchScheduleDto()

    @PatchMapping("/{scheduleId}")
    fun updateSchedule(
        @PathVariable("scheduleId") scheduleId: ClubMatchScheduleId,
        @Valid @RequestBody body: UpdateClubMatchScheduleRequest
    ): ClubMatchScheduleDto =
        clubMatchScheduleService.updateSchedule(
            scheduleId = scheduleId,
            userId = requestUserId,
            matchDayOfWeek = body.matchDayOfWeek,
            matchTime = body.matchTime,
            timeZone = body.timeZone,
            format = body.format,
            isActive = body.isActive
        ).toClubMatchScheduleDto()
}
