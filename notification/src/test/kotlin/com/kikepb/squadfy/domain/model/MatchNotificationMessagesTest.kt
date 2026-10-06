package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.events.match.MatchEvent
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MatchNotificationMessagesTest {

    private val clubId = UUID.randomUUID()
    private val matchId = UUID.randomUUID()
    private val announcementId = UUID.randomUUID()
    private val users = List(4) { UUID.randomUUID() }

    // Thursday 15 October 2026, 20:00 in Madrid (UTC+2)
    private val kickoff = Instant.parse("2026-10-15T18:00:00Z")
    private val closesAt = Instant.parse("2026-10-14T20:00:00Z")

    @Test
    fun `opening push uses the club name and the local match date in Spanish`() {
        val push = MatchNotificationMessages.of(
            MatchEvent.AnnouncementOpened(clubId, "Squadfy FC", matchId, kickoff, "Europe/Madrid", announcementId, closesAt, users)
        ).single()

        assertEquals("Squadfy FC: convocatoria abierta", push.title)
        assertEquals("Partido el jueves 15 de octubre a las 20:00. ¡Apúntate!", push.body)
        assertEquals(users, push.userIds)
        assertEquals(announcementId.toString(), push.data["announcementId"])
        assertEquals("match.announcement.opened", push.data["type"])
    }

    @Test
    fun `closing reminder tells how many places are left and when it closes`() {
        val many = MatchNotificationMessages.of(
            MatchEvent.AnnouncementClosingSoon(clubId, "Squadfy FC", matchId, kickoff, "Europe/Madrid", announcementId, closesAt, 3, users)
        ).single()
        val one = MatchNotificationMessages.of(
            MatchEvent.AnnouncementClosingSoon(clubId, "Squadfy FC", matchId, kickoff, "Europe/Madrid", announcementId, closesAt, 1, users)
        ).single()

        assertEquals("Squadfy FC: quedan 3 plazas", many.title)
        assertEquals("La convocatoria cierra el miércoles 14 a las 22:00. Partido el jueves 15 de octubre a las 20:00.", many.body)
        assertEquals("Squadfy FC: queda 1 plaza", one.title)
    }

    @Test
    fun `each team gets its own push`() {
        val pushes = MatchNotificationMessages.of(
            MatchEvent.TeamsPublished(clubId, "Squadfy FC", matchId, kickoff, "Europe/Madrid", users.take(2), users.drop(2))
        )

        assertEquals(2, pushes.size)
        assertEquals(users.take(2), pushes[0].userIds)
        assertTrue(pushes[0].body.startsWith("Juegas en el equipo A"))
        assertTrue(pushes[1].body.startsWith("Juegas en el equipo B"))
    }

    @Test
    fun `only waitlist promotions ignore muted clubs`() {
        val promoted = MatchEvent.PromotedFromWaitlist(clubId, "Squadfy FC", matchId, kickoff, "Europe/Madrid", announcementId, users.first())
        val cancelled = MatchEvent.MatchCancelled(clubId, "Squadfy FC", matchId, kickoff, "Europe/Madrid", users)

        assertTrue(MatchNotificationMessages.ignoresMute(promoted))
        assertFalse(MatchNotificationMessages.ignoresMute(cancelled))
        assertEquals("Squadfy FC: ¡tienes plaza!", MatchNotificationMessages.of(promoted).single().title)
        assertEquals("Squadfy FC: partido cancelado", MatchNotificationMessages.of(cancelled).single().title)
    }

    @Test
    fun `pushes without recipients are dropped and a blank club name falls back to Squadfy`() {
        val pushes = MatchNotificationMessages.of(
            MatchEvent.TeamsPublished(clubId, "", matchId, kickoff, "Europe/Madrid", users, emptyList())
        )

        assertEquals(1, pushes.size)
        assertEquals("Squadfy: equipos publicados", pushes.single().title)
    }
}
