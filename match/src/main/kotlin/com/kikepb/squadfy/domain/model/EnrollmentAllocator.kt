package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.model.MatchAnnouncementEntryModel.EntryStatus
import java.time.Instant
import java.util.UUID

/**
 * Assigns confirmed places of an announcement (spec 008 RN-A2, RN-A3): members first in enrollment
 * order, then guests in the order they were added. The first `maxPlayers` are confirmed, the rest
 * waitlisted, so a member joining a full announcement pushes the last confirmed guest to the waitlist.
 */
object EnrollmentAllocator {

    data class Participant(
        val entryId: UUID,
        val isGuest: Boolean,
        val enrolledAt: Instant
    )

    fun allocate(participants: List<Participant>, maxPlayers: Int): Map<UUID, EntryStatus> =
        participants
            .sortedWith(compareBy<Participant>({ it.isGuest }, { it.enrolledAt }, { it.entryId.toString() }))
            .mapIndexed { index, participant ->
                participant.entryId to if (index < maxPlayers) EntryStatus.CONFIRMED else EntryStatus.WAITLISTED
            }
            .toMap()
}
