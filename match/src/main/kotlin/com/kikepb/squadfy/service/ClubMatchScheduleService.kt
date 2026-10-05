package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.exception.ClubMatchScheduleAlreadyExistsException
import com.kikepb.squadfy.domain.exception.ClubMatchScheduleNotFoundException
import com.kikepb.squadfy.domain.exception.InvalidClubMatchScheduleException
import com.kikepb.squadfy.domain.model.ClubMatchScheduleModel
import com.kikepb.squadfy.domain.model.MatchFormat
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMatchScheduleId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.ClubMatchScheduleEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toClubMatchScheduleModel
import com.kikepb.squadfy.infrastructure.database.repositories.ClubMatchScheduleRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId

@Service
class ClubMatchScheduleService(
    private val clubMatchScheduleRepository: ClubMatchScheduleRepository,
    private val matchPlanningService: MatchPlanningService,
    private val clubAccessGuard: ClubAccessGuard
) {

    @Transactional
    fun createSchedule(
        clubId: ClubId,
        userId: UserId,
        matchDayOfWeek: DayOfWeek,
        matchTime: LocalTime,
        timeZone: String,
        format: MatchFormat
    ): ClubMatchScheduleModel {
        clubAccessGuard.requireManager(clubId = clubId, userId = userId)

        if (clubMatchScheduleRepository.existsByClubId(clubId = clubId)) {
            throw ClubMatchScheduleAlreadyExistsException()
        }

        val schedule = clubMatchScheduleRepository.saveAndFlush(
            ClubMatchScheduleEntity(
                clubId = clubId,
                matchDayOfWeek = matchDayOfWeek,
                matchTime = matchTime,
                timeZone = parseZone(timeZone).id,
                format = format,
                maxPlayers = format.maxPlayers
            )
        ).toClubMatchScheduleModel()

        matchPlanningService.planNextMatch(schedule = schedule)
        return schedule
    }

    fun getScheduleByClub(clubId: ClubId, userId: UserId): ClubMatchScheduleModel {
        clubAccessGuard.requireMember(clubId = clubId, userId = userId)
        return clubMatchScheduleRepository.findByClubId(clubId = clubId)?.toClubMatchScheduleModel()
            ?: throw ClubMatchScheduleNotFoundException()
    }

    @Transactional
    fun updateSchedule(
        scheduleId: ClubMatchScheduleId,
        userId: UserId,
        matchDayOfWeek: DayOfWeek?,
        matchTime: LocalTime?,
        timeZone: String?,
        format: MatchFormat?,
        isActive: Boolean?
    ): ClubMatchScheduleModel {
        val entity = clubMatchScheduleRepository.findByIdOrNull(scheduleId)
            ?: throw ClubMatchScheduleNotFoundException()
        clubAccessGuard.requireManager(clubId = entity.clubId, userId = userId)

        matchDayOfWeek?.let { entity.matchDayOfWeek = it }
        matchTime?.let { entity.matchTime = it }
        timeZone?.let { entity.timeZone = parseZone(it).id }
        format?.let {
            entity.format = it
            entity.maxPlayers = it.maxPlayers
        }
        isActive?.let { entity.isActive = it }

        val schedule = clubMatchScheduleRepository.saveAndFlush(entity).toClubMatchScheduleModel()
        if (schedule.isActive) matchPlanningService.planNextMatch(schedule = schedule)
        return schedule
    }

    fun findAllActive(): List<ClubMatchScheduleModel> =
        clubMatchScheduleRepository.findAllByIsActive(isActive = true)
            .map { it.toClubMatchScheduleModel() }

    private fun parseZone(timeZone: String): ZoneId =
        try {
            ZoneId.of(timeZone.trim())
        } catch (e: DateTimeException) {
            throw InvalidClubMatchScheduleException("Unknown time zone: $timeZone")
        }
}
