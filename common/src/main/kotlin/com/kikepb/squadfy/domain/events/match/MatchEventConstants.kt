package com.kikepb.squadfy.domain.events.match

object MatchEventConstants {
    const val MATCH_EXCHANGE = "match.events"

    const val ANNOUNCEMENT_OPENED = "match.announcement.opened"
    const val ANNOUNCEMENT_CLOSING_SOON = "match.announcement.closing_soon"
    const val TEAMS_PUBLISHED = "match.teams.published"
    const val MATCH_CANCELLED = "match.cancelled"
    const val WAITLIST_PROMOTED = "match.waitlist.promoted"
}
