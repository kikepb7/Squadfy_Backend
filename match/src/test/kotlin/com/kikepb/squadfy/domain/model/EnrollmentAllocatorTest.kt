package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.model.EnrollmentAllocator.Participant
import com.kikepb.squadfy.domain.model.MatchAnnouncementEntryModel.EntryStatus.CONFIRMED
import com.kikepb.squadfy.domain.model.MatchAnnouncementEntryModel.EntryStatus.WAITLISTED
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class EnrollmentAllocatorTest {

    private val start = Instant.parse("2026-10-05T10:00:00Z")

    private fun member(minute: Long) = Participant(UUID.randomUUID(), isGuest = false, enrolledAt = start.plusSeconds(minute * 60))
    private fun guest(minute: Long) = Participant(UUID.randomUUID(), isGuest = true, enrolledAt = start.plusSeconds(minute * 60))

    @Test
    fun `guests only take places left by members, whatever their enrollment time`() {
        val earlyGuest = guest(0)
        val lateGuest = guest(1)
        val members = (2L..4L).map { member(it) }

        val allocation = EnrollmentAllocator.allocate(participants = listOf(lateGuest, earlyGuest) + members, maxPlayers = 4)

        members.forEach { assertEquals(CONFIRMED, allocation[it.entryId]) }
        assertEquals(CONFIRMED, allocation[earlyGuest.entryId])
        assertEquals(WAITLISTED, allocation[lateGuest.entryId])
    }

    @Test
    fun `members beyond the capacity wait ahead of every guest`() {
        val members = (0L..2L).map { member(it) }
        val guest = guest(-10)

        val allocation = EnrollmentAllocator.allocate(participants = members + guest, maxPlayers = 2)

        assertEquals(listOf(CONFIRMED, CONFIRMED, WAITLISTED), members.map { allocation[it.entryId] })
        assertEquals(WAITLISTED, allocation[guest.entryId])
    }

    @Test
    fun `nobody is confirmed without places`() {
        assertEquals(emptyMap(), EnrollmentAllocator.allocate(participants = emptyList(), maxPlayers = 10))
        assertEquals(WAITLISTED, EnrollmentAllocator.allocate(participants = listOf(member(0)), maxPlayers = 0).values.single())
    }
}
