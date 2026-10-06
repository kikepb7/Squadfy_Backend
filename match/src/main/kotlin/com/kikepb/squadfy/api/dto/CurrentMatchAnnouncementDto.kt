package com.kikepb.squadfy.api.dto

import com.kikepb.squadfy.domain.model.CurrentMatchAnnouncementModel.MyEnrollmentStatus
import java.time.Instant

/** Home screen: the announcement of the club's next match and where the requester stands. */
data class CurrentMatchAnnouncementDto(
    val announcement: MatchAnnouncementDto,
    val matchScheduledAt: Instant,
    val myStatus: MyEnrollmentStatus,
    /** 1-based position on the waitlist; null unless myStatus is WAITLISTED. */
    val myWaitlistPosition: Int?
)
