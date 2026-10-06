package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.model.MatchAnnouncementEntryModel.EntryStatus
import com.kikepb.squadfy.domain.type.ClubMemberId
import java.time.Instant

/** Announcement of the club's next scheduled match, seen by one member (spec 007 R-7). */
data class CurrentMatchAnnouncementModel(
    val announcement: MatchAnnouncementModel,
    val matchScheduledAt: Instant,
    val myStatus: MyEnrollmentStatus,
    /** 1-based position on the waitlist, only when [myStatus] is WAITLISTED. */
    val myWaitlistPosition: Int?
) {
    enum class MyEnrollmentStatus {
        NOT_ENROLLED,
        CONFIRMED,
        WAITLISTED
    }

    companion object {
        fun of(announcement: MatchAnnouncementModel, matchScheduledAt: Instant, memberId: ClubMemberId): CurrentMatchAnnouncementModel {
            val myEntry = announcement.entries.firstOrNull { it.clubMemberId == memberId }
            val waitlistPosition = announcement.waitlistEntries
                .sortedBy { it.enrolledAt }
                .indexOfFirst { it.clubMemberId == memberId }
                .takeIf { it >= 0 }
                ?.plus(1)

            return CurrentMatchAnnouncementModel(
                announcement = announcement,
                matchScheduledAt = matchScheduledAt,
                myStatus = when (myEntry?.status) {
                    null -> MyEnrollmentStatus.NOT_ENROLLED
                    EntryStatus.CONFIRMED -> MyEnrollmentStatus.CONFIRMED
                    EntryStatus.WAITLISTED -> MyEnrollmentStatus.WAITLISTED
                },
                myWaitlistPosition = waitlistPosition
            )
        }
    }
}
