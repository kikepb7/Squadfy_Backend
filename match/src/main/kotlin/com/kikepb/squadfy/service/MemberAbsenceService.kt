package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.exception.InvalidMemberAbsenceException
import com.kikepb.squadfy.domain.exception.MemberAbsenceNotFoundException
import com.kikepb.squadfy.domain.model.MatchModel.MatchStatus.SCHEDULED
import com.kikepb.squadfy.domain.model.MemberAbsenceModel
import com.kikepb.squadfy.domain.type.ClubId
import com.kikepb.squadfy.domain.type.ClubMemberId
import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.database.entities.DEFAULT_CLUB_TIME_ZONE
import com.kikepb.squadfy.infrastructure.database.entities.MemberAbsenceEntity
import com.kikepb.squadfy.infrastructure.database.mappers.toMemberAbsenceModel
import com.kikepb.squadfy.infrastructure.database.repositories.ClubMatchScheduleRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MatchRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MemberAbsenceRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

/** Periods in which a member will not play (spec 008 RN-C). Dates are local dates of the club. */
@Service
class MemberAbsenceService(
    private val memberAbsenceRepository: MemberAbsenceRepository,
    private val matchRepository: MatchRepository,
    private val clubMatchScheduleRepository: ClubMatchScheduleRepository,
    private val matchAnnouncementService: MatchAnnouncementService,
    private val clubAccessGuard: ClubAccessGuard,
    private val clock: Clock
) {

    /** Absences of the club overlapping [from]–[to] (every absence when both are null), visible to members (RN-C5). */
    fun getAbsences(clubId: ClubId, userId: UserId, from: LocalDate?, to: LocalDate?): List<MemberAbsenceModel> {
        clubAccessGuard.requireMember(clubId = clubId, userId = userId)
        val absences = if (from == null && to == null) {
            memberAbsenceRepository.findAllByClubIdOrderByFromDateAsc(clubId = clubId)
        } else {
            memberAbsenceRepository.findAllByClubIdAndFromDateLessThanEqualAndToDateGreaterThanEqualOrderByFromDateAsc(
                clubId = clubId,
                to = to ?: NO_LIMIT_TO,
                from = from ?: NO_LIMIT_FROM
            )
        }
        return absences.map { it.toMemberAbsenceModel() }
    }

    /** Registers an absence and withdraws the member from the open announcements it covers (RN-C2). */
    @Transactional
    fun createAbsence(clubId: ClubId, userId: UserId, fromDate: LocalDate, toDate: LocalDate, reason: String?): MemberAbsenceModel {
        val memberId = clubAccessGuard.requireMember(clubId = clubId, userId = userId).memberId
        val zone = zoneOf(clubId = clubId)
        val today = clock.instant().atZone(zone).toLocalDate()

        if (toDate.isBefore(fromDate)) throw InvalidMemberAbsenceException("toDate cannot be before fromDate")
        if (toDate.isBefore(today)) throw InvalidMemberAbsenceException("The absence must end today or later")
        if (ChronoUnit.DAYS.between(fromDate, toDate) >= MAX_ABSENCE_DAYS) {
            throw InvalidMemberAbsenceException("An absence can last at most $MAX_ABSENCE_DAYS days")
        }

        val absence = memberAbsenceRepository.saveAndFlush(
            MemberAbsenceEntity(clubId = clubId, clubMemberId = memberId, fromDate = fromDate, toDate = toDate, reason = reason)
        )

        val matchIds = matchRepository.findAllByClubIdAndStatusAndScheduledAtGreaterThanEqualAndScheduledAtLessThan(
            clubId = clubId,
            status = SCHEDULED,
            from = fromDate.atStartOfDay(zone).toInstant(),
            to = toDate.plusDays(1).atStartOfDay(zone).toInstant()
        ).map { requireNotNull(it.id) }
        if (matchIds.isNotEmpty()) {
            matchAnnouncementService.withdrawForAbsence(clubId = clubId, clubMemberId = memberId, matchIdsInAbsence = matchIds)
        }
        return absence.toMemberAbsenceModel()
    }

    /** A member deletes their own absences only. */
    @Transactional
    fun deleteAbsence(clubId: ClubId, userId: UserId, absenceId: UUID) {
        val memberId = clubAccessGuard.requireMember(clubId = clubId, userId = userId).memberId
        val absence = memberAbsenceRepository.findByIdOrNull(absenceId)
            ?.takeIf { it.clubId == clubId && it.clubMemberId == memberId }
            ?: throw MemberAbsenceNotFoundException()
        memberAbsenceRepository.delete(absence)
    }

    /** Members absent on the local date of a match, excluded from its opening and reminder pushes (RN-C3). */
    fun absentMemberIds(clubId: ClubId, matchAt: Instant): Set<ClubMemberId> {
        val date = matchAt.atZone(zoneOf(clubId = clubId)).toLocalDate()
        return memberAbsenceRepository.findAllByClubIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(
            clubId = clubId,
            date = date,
            sameDate = date
        ).map { it.clubMemberId }.toSet()
    }

    private fun zoneOf(clubId: ClubId): ZoneId =
        ZoneId.of(clubMatchScheduleRepository.findByClubId(clubId = clubId)?.timeZone ?: DEFAULT_CLUB_TIME_ZONE)

    private companion object {
        const val MAX_ABSENCE_DAYS = 366L
        val NO_LIMIT_FROM: LocalDate = LocalDate.of(2000, 1, 1)
        val NO_LIMIT_TO: LocalDate = LocalDate.of(9999, 12, 31)
    }
}
