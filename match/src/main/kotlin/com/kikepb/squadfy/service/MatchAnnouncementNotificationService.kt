package com.kikepb.squadfy.service

import com.kikepb.squadfy.domain.model.MatchAnnouncementEntryModel.EntryStatus.CONFIRMED
import com.kikepb.squadfy.domain.model.MatchAnnouncementModel.MatchAnnouncementStatus.OPEN
import com.kikepb.squadfy.infrastructure.database.entities.MatchAnnouncementEntity
import com.kikepb.squadfy.infrastructure.database.repositories.MatchAnnouncementEntryRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MatchAnnouncementRepository
import com.kikepb.squadfy.infrastructure.database.repositories.MatchRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration

/**
 * Time-based notifications of the announcement (spec 005 RN-1, RN-2, RN-5): each one is emitted at
 * most once, marked under a row lock in the same transaction as its publication.
 */
@Service
class MatchAnnouncementNotificationService(
    private val matchAnnouncementRepository: MatchAnnouncementRepository,
    private val matchAnnouncementEntryRepository: MatchAnnouncementEntryRepository,
    private val matchRepository: MatchRepository,
    private val matchNotificationPublisher: MatchNotificationPublisher,
    private val clock: Clock,
    @param:Value("\${squadfy.notifications.closing-reminder-hours:24}")
    private val closingReminderHours: Long
) {

    @Transactional
    fun notifyOpenedAnnouncements(): Int {
        val now = clock.instant()
        return matchAnnouncementRepository.findAllByStatusAndOpensAtLessThanEqualAndOpenedNotifiedAtIsNull(status = OPEN, now = now)
            .count { candidate ->
                val announcement = lockPending(candidate) { it.openedNotifiedAt == null } ?: return@count false
                announcement.openedNotifiedAt = now

                matchNotificationPublisher.announcementOpened(
                    clubId = announcement.clubId,
                    matchId = announcement.matchId,
                    matchScheduledAt = matchScheduledAt(announcement),
                    announcementId = requireNotNull(announcement.id),
                    closesAt = announcement.closesAt
                )
                true
            }
    }

    @Transactional
    fun sendClosingReminders(): Int {
        val now = clock.instant()
        val reminderLead = Duration.ofHours(closingReminderHours)

        return matchAnnouncementRepository.findAllByStatusAndClosesAtAfterAndClosingReminderSentAtIsNull(status = OPEN, now = now)
            .filter { !now.isBefore(it.closesAt.minus(reminderLead)) }
            .count { candidate ->
                val announcement = lockPending(candidate) { it.closingReminderSentAt == null } ?: return@count false
                val reminderTime = announcement.closesAt.minus(reminderLead)

                // Opened within the reminder window: members just got the opening push (RN-2).
                if (announcement.opensAt.isAfter(reminderTime)) {
                    announcement.closingReminderSentAt = now
                    return@count false
                }

                val entries = matchAnnouncementEntryRepository.findAllByMatchAnnouncementIdOrderByEnrolledAtAsc(
                    matchAnnouncementId = requireNotNull(announcement.id)
                )
                val freePlaces = announcement.maxPlayers - entries.count { it.status == CONFIRMED }
                // Full: keep it pending, a place may still free up before closing.
                if (freePlaces <= 0) return@count false

                announcement.closingReminderSentAt = now
                matchNotificationPublisher.announcementClosingSoon(
                    clubId = announcement.clubId,
                    matchId = announcement.matchId,
                    matchScheduledAt = matchScheduledAt(announcement),
                    announcementId = requireNotNull(announcement.id),
                    closesAt = announcement.closesAt,
                    freePlaces = freePlaces,
                    enrolledMemberIds = entries.map { it.clubMemberId }.toSet()
                )
                true
            }
    }

    /** Re-reads the announcement under lock so concurrent runs or instances notify only once. */
    private fun lockPending(
        candidate: MatchAnnouncementEntity,
        isPending: (MatchAnnouncementEntity) -> Boolean
    ): MatchAnnouncementEntity? =
        matchAnnouncementRepository.findByIdForUpdate(id = requireNotNull(candidate.id))?.takeIf(isPending)

    private fun matchScheduledAt(announcement: MatchAnnouncementEntity) =
        requireNotNull(matchRepository.findByIdOrNull(announcement.matchId)).scheduledAt
}
