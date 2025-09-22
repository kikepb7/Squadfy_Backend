package com.kikepb.squadfy.domain.events

import java.time.Instant

interface SquadfyEvent {
    val eventId: String
    val eventKey: String
    val occurredAt: Instant
    val exchange: String
}