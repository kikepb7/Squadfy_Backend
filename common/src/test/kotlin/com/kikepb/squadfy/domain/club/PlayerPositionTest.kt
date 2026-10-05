package com.kikepb.squadfy.domain.club

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlayerPositionTest {

    @Test
    fun `parses enum names and common legacy aliases`() {
        assertEquals(PlayerPosition.GOALKEEPER, PlayerPosition.fromRaw(" goalkeeper "))
        assertEquals(PlayerPosition.GOALKEEPER, PlayerPosition.fromRaw("Portero"))
        assertEquals(PlayerPosition.DEFENDER, PlayerPosition.fromRaw("defensa"))
        assertEquals(PlayerPosition.MIDFIELDER, PlayerPosition.fromRaw("MF"))
        assertEquals(PlayerPosition.FORWARD, PlayerPosition.fromRaw("delantero"))
    }

    @Test
    fun `unknown or empty values are null`() {
        assertNull(PlayerPosition.fromRaw(null))
        assertNull(PlayerPosition.fromRaw(""))
        assertNull(PlayerPosition.fromRaw("libero"))
    }
}
