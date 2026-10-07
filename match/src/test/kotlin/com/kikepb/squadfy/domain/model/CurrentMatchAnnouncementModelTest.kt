package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.model.CurrentMatchAnnouncementModel.MyEnrollmentStatus
import com.kikepb.squadfy.domain.model.MatchAnnouncementEntryModel.EntryStatus
import com.kikepb.squadfy.domain.model.MatchAnnouncementModel.MatchAnnouncementStatus
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CurrentMatchAnnouncementModelTest {

    private val now = Instant.parse("2026-10-06T10:00:00Z")
    private val announcementId = UUID.randomUUID()

    private fun entry(status: EntryStatus, minutesAfter: Long) = MatchAnnouncementEntryModel(
        id = UUID.randomUUID(),
        matchAnnouncementId = announcementId,
        clubMemberId = UUID.randomUUID(),
        status = status,
        enrolledAt = now.plusSeconds(minutesAfter * 60)
    )

    private fun announcement(entries: List<MatchAnnouncementEntryModel>) = MatchAnnouncementModel(
        id = announcementId,
        matchId = UUID.randomUUID(),
        clubId = UUID.randomUUID(),
        maxPlayers = 10,
        opensAt = now,
        closesAt = now.plusSeconds(86_400),
        drawAt = now.plusSeconds(86_400),
        status = MatchAnnouncementStatus.OPEN,
        entries = entries,
        createdAt = now,
        updatedAt = now
    )

    @Test
    fun `waitlisted member gets their 1-based position by enrollment time`() {
        val confirmed = entry(EntryStatus.CONFIRMED, 0)
        val firstWaiting = entry(EntryStatus.WAITLISTED, 5)
        val secondWaiting = entry(EntryStatus.WAITLISTED, 10)
        val model = announcement(listOf(secondWaiting, confirmed, firstWaiting))

        val current = CurrentMatchAnnouncementModel.of(model, now.plusSeconds(3_600), requireNotNull(secondWaiting.clubMemberId))

        assertEquals(MyEnrollmentStatus.WAITLISTED, current.myStatus)
        assertEquals(2, current.myWaitlistPosition)
    }

    @Test
    fun `confirmed and not enrolled members have no waitlist position`() {
        val confirmed = entry(EntryStatus.CONFIRMED, 0)
        val model = announcement(listOf(confirmed))

        val mine = CurrentMatchAnnouncementModel.of(model, now, requireNotNull(confirmed.clubMemberId))
        val other = CurrentMatchAnnouncementModel.of(model, now, UUID.randomUUID())

        assertEquals(MyEnrollmentStatus.CONFIRMED, mine.myStatus)
        assertNull(mine.myWaitlistPosition)
        assertEquals(MyEnrollmentStatus.NOT_ENROLLED, other.myStatus)
        assertNull(other.myWaitlistPosition)
    }
}
