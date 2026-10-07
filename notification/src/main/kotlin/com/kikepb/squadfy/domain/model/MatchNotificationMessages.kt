package com.kikepb.squadfy.domain.model

import com.kikepb.squadfy.domain.events.match.MatchEvent
import com.kikepb.squadfy.domain.type.UserId
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Push texts of the match cycle (spec 005 RN-8): Spanish, dates in the club's time zone. */
object MatchNotificationMessages {

    data class MatchPush(
        val userIds: List<UserId>,
        val title: String,
        val body: String,
        val data: Map<String, String>,
        val collapseKey: String
    )

    private val SPANISH: Locale = Locale.of("es", "ES")
    private val MATCH_DATE = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'a las' HH:mm", SPANISH)
    private val CLOSING_DAY = DateTimeFormatter.ofPattern("EEEE d", SPANISH)
    private val CLOSING_TIME = DateTimeFormatter.ofPattern("HH:mm", SPANISH)

    fun of(event: MatchEvent): List<MatchPush> {
        val club = event.clubName.ifBlank { "Squadfy" }
        val matchDate = format(event.matchScheduledAt, event.timeZone, MATCH_DATE)

        fun push(userIds: List<UserId>, title: String, body: String, extra: Map<String, String> = emptyMap()) =
            MatchPush(
                userIds = userIds,
                title = "$club: $title",
                body = body,
                data = mapOf(
                    "type" to event.eventKey,
                    "clubId" to event.clubId.toString(),
                    "matchId" to event.matchId.toString()
                ) + extra,
                collapseKey = "${event.eventKey}-${event.matchId}"
            )

        return when (event) {
            is MatchEvent.AnnouncementOpened -> listOf(
                push(
                    userIds = event.recipientUserIds,
                    title = "convocatoria abierta",
                    body = "Partido el $matchDate. ¡Apúntate!",
                    extra = mapOf("announcementId" to event.announcementId.toString())
                )
            )
            is MatchEvent.AnnouncementClosingSoon -> listOf(
                push(
                    userIds = event.recipientUserIds,
                    title = if (event.freePlaces == 1) "queda 1 plaza" else "quedan ${event.freePlaces} plazas",
                    body = "La convocatoria cierra el ${format(event.closesAt, event.timeZone, CLOSING_DAY)} " +
                        "a las ${format(event.closesAt, event.timeZone, CLOSING_TIME)}. Partido el $matchDate.",
                    extra = mapOf("announcementId" to event.announcementId.toString())
                )
            )
            is MatchEvent.TeamsPublished -> listOf(
                push(event.teamAUserIds, "equipos publicados", "Juegas en el equipo A · $matchDate", mapOf("team" to "A")),
                push(event.teamBUserIds, "equipos publicados", "Juegas en el equipo B · $matchDate", mapOf("team" to "B"))
            )
            is MatchEvent.MatchCancelled -> listOf(
                push(event.recipientUserIds, "partido cancelado", "Se ha cancelado el partido del $matchDate.")
            )
            is MatchEvent.MatchRescheduled -> listOf(
                push(
                    userIds = event.recipientUserIds,
                    title = "partido cambiado",
                    body = "El partido del ${format(event.previousScheduledAt, event.timeZone, MATCH_DATE)} pasa al $matchDate."
                )
            )
            is MatchEvent.PromotedFromWaitlist -> listOf(
                push(
                    userIds = listOf(event.userId),
                    title = "¡tienes plaza!",
                    body = "Has pasado de la lista de espera a convocado para el partido del $matchDate.",
                    extra = mapOf("announcementId" to event.announcementId.toString())
                )
            )
        }.filter { it.userIds.isNotEmpty() }
    }

    /** Promotion concerns the user directly, so it ignores club mutes (spec 005 RN-7). */
    fun ignoresMute(event: MatchEvent): Boolean = event is MatchEvent.PromotedFromWaitlist

    private fun format(instant: Instant, timeZone: String, formatter: DateTimeFormatter): String =
        formatter.withZone(ZoneId.of(timeZone)).format(instant)
}
