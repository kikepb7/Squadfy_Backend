package com.kikepb.squadfy.domain.model

enum class MatchFormat(val playersPerSide: Int) {
    FIVE_A_SIDE(5),
    SEVEN_A_SIDE(7),
    ELEVEN_A_SIDE(11);

    /** Confirmed places of the announcement; further enrollments go to the waitlist. */
    val maxPlayers: Int get() = playersPerSide * 2
}
