package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.exception.ClubMatchScheduleAlreadyExistsException
import com.kikepb.squadfy.domain.exception.ClubMatchScheduleNotFoundException
import com.kikepb.squadfy.domain.model.ClubMatchScheduleModel
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMatchScheduleId
import com.kikepb.squadfy.infrastructure.database.entities.ClubMatchScheduleEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toClubMatchScheduleModel
import com.kikepb.squadfy.infrastructure.database.repositories.ClubMatchScheduleRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalTime

@Service
class ClubMatchScheduleService(
    private val clubMatchScheduleRepository: ClubMatchScheduleRepository
) {

    @Transactional
    fun createSchedule(
        clubId: ClubId,
        matchDayOfWeek: DayOfWeek,
        matchTime: LocalTime,
        matchAnnouncementOpenDaysBeforeMatch: Int,
        maxPlayers: Int
    ): ClubMatchScheduleModel {
        if (clubMatchScheduleRepository.existsByClubId(clubId = clubId)) {
            throw ClubMatchScheduleAlreadyExistsException()
        }

        val entity = clubMatchScheduleRepository.saveAndFlush(
            ClubMatchScheduleEntity(
                clubId = clubId,
                matchDayOfWeek = matchDayOfWeek,
                matchTime = matchTime,
                matchAnnouncementOpenDaysBeforeMatch = matchAnnouncementOpenDaysBeforeMatch,
                maxPlayers = maxPlayers
            )
        )
        return entity.toClubMatchScheduleModel()
    }

    fun getScheduleByClub(clubId: ClubId): ClubMatchScheduleModel =
        clubMatchScheduleRepository.findByClubId(clubId = clubId)?.toClubMatchScheduleModel()
            ?: throw ClubMatchScheduleNotFoundException()

    @Transactional
    fun updateSchedule(
        scheduleId: ClubMatchScheduleId,
        matchDayOfWeek: DayOfWeek?,
        matchTime: LocalTime?,
        matchAnnouncementOpenDaysBeforeMatch: Int?,
        maxPlayers: Int?,
        isActive: Boolean?
    ): ClubMatchScheduleModel {
        val entity = clubMatchScheduleRepository.findByIdOrNull(scheduleId)
            ?: throw ClubMatchScheduleNotFoundException()

        matchDayOfWeek?.let { entity.matchDayOfWeek = it }
        matchTime?.let { entity.matchTime = it }
        matchAnnouncementOpenDaysBeforeMatch?.let { entity.matchAnnouncementOpenDaysBeforeMatch = it }
        maxPlayers?.let { entity.maxPlayers = it }
        isActive?.let { entity.isActive = it }

        clubMatchScheduleRepository.saveAndFlush(entity)
        return entity.toClubMatchScheduleModel()
    }

    fun findAllActive(): List<ClubMatchScheduleModel> =
        clubMatchScheduleRepository.findAllByIsActive(isActive = true)
            .map { it.toClubMatchScheduleModel() }
}
