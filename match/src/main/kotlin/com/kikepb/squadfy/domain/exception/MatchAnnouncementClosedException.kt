package com.kikepb.squadfy.domain.exception

class MatchAnnouncementClosedException(
    message: String = "The match announcement is no longer open for enrollment"
) : RuntimeException(message)
