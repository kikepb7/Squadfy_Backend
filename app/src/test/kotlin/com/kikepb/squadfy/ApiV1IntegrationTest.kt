package com.kikepb.squadfy

import com.kikepb.squadfy.domain.events.user.UserEvent
import com.kikepb.squadfy.infrastructure.message_queue.EventPublisher
import com.kikepb.squadfy.testing.InfrastructureTestContainersConfiguration
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.context.annotation.Import
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Contract of spec 007: every endpoint under /api/v1, members' emails hidden, OpenAPI published. */
@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    properties = [
        "jwt.secret=c3F1YWRmeS10ZXN0LXNlY3JldC1zcXVhZGZ5LXRlc3Qtc2VjcmV0LTEyMzQ1Ng==",
        "firebase.enabled=false",
        "springdoc.api-docs.enabled=true"
    ]
)
@Import(InfrastructureTestContainersConfiguration::class)
class ApiV1IntegrationTest : ApiIntegrationTestSupport() {

    @Autowired @Qualifier("requestMappingHandlerMapping") lateinit var handlerMapping: RequestMappingHandlerMapping
    @Autowired lateinit var eventPublisher: EventPublisher

    @Test
    fun `club, schedule, announcement and ratings flow through v1`() {
        val (_, token) = newUser()

        val me = call("GET", "/api/v1/me", token)
        assertEquals(200, me.status)
        assertTrue(me.json["email"].asText().endsWith("@squadfy.test"))

        val club = call("POST", "/api/v1/clubs", token, """{"name":"Squadfy FC"}""")
        assertEquals(201, club.status, club.body)
        val clubId = club.json["id"].asText()
        // Contract for the mobile app: instants are ISO-8601 strings in UTC
        assertTrue(Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?Z""").matches(club.json["createdAt"].asText()), club.body)

        val members = call("GET", "/api/v1/clubs/$clubId/members", token)
        assertEquals(200, members.status)
        assertEquals(1, members.json.size())
        assertFalse(members.json[0].has("email"), "Members must not expose emails: ${members.body}")

        val schedule = call(
            "POST", "/api/v1/clubs/$clubId/schedule", token,
            """{"matchDayOfWeek":"THURSDAY","matchTime":"20:00:00","format":"FIVE_A_SIDE"}"""
        )
        assertEquals(201, schedule.status, schedule.body)
        assertEquals(10, schedule.json["maxPlayers"].asInt())

        val matches = call("GET", "/api/v1/clubs/$clubId/matches?status=SCHEDULED", token)
        assertEquals(200, matches.status)
        assertEquals(1, matches.json.size())

        val current = call("GET", "/api/v1/clubs/$clubId/announcements/current", token)
        assertEquals(200, current.status, current.body)
        assertEquals("NOT_ENROLLED", current.json["myStatus"].asText())
        val announcementId = current.json["announcement"]["id"].asText()

        assertEquals(200, call("POST", "/api/v1/announcements/$announcementId/enrollment", token).status)
        assertEquals("CONFIRMED", call("GET", "/api/v1/clubs/$clubId/announcements/current", token).json["myStatus"].asText())
        assertEquals(200, call("DELETE", "/api/v1/announcements/$announcementId/enrollment", token).status)

        assertEquals(60, schedule.json["matchDurationMinutes"].asInt())
        val stats = call("GET", "/api/v1/clubs/$clubId/stats?sortBy=WINS", token)
        assertEquals(200, stats.status, stats.body)
        assertEquals(1, stats.json[0]["rank"].asInt())
        assertEquals(0, call("GET", "/api/v1/clubs/$clubId/stats/me", token).json["matchesPlayed"].asInt())
        assertFalse(members.json[0].has("goalsScored"), "Counters moved to /stats")

        val ratings = call("GET", "/api/v1/clubs/$clubId/ratings", token)
        assertEquals(200, ratings.status)
        assertEquals(1, ratings.json[0]["rank"].asInt())
        assertEquals(1, call("GET", "/api/v1/clubs/$clubId/ratings/me", token).json["totalPlayers"].asInt())
    }

    @Test
    fun `a confirmed player who leaves the club frees the place for the first on the waitlist`() {
        val (_, ownerToken) = newUser()
        val club = call("POST", "/api/v1/clubs", ownerToken, """{"name":"Waitlist FC"}""").json
        val clubId = club["id"].asText()
        call("POST", "/api/v1/clubs/$clubId/schedule", ownerToken, """{"matchDayOfWeek":"THURSDAY","matchTime":"20:00:00","format":"FIVE_A_SIDE"}""")

        val players = List(11) { newUser() }
        players.forEach { (_, token) ->
            assertEquals(200, call("POST", "/api/v1/clubs/join", token, """{"invitationCode":"${club["invitationCode"].asText()}"}""").status)
        }
        val announcementId = call("GET", "/api/v1/clubs/$clubId/announcements/current", ownerToken).json["announcement"]["id"].asText()
        players.forEach { (_, token) -> call("POST", "/api/v1/announcements/$announcementId/enrollment", token) }

        val before = call("GET", "/api/v1/announcements/$announcementId", ownerToken).json
        assertEquals(10, before["confirmedCount"].asInt())
        val firstWaitlisted = before["waitlist"][0]["clubMemberId"].asText()
        val leaverToken = players.first().second
        val leaverMemberId = before["entries"][0]["clubMemberId"].asText()

        assertEquals(204, call("DELETE", "/api/v1/clubs/$clubId/members/me", leaverToken).status)

        await().atMost(Duration.ofSeconds(15)).untilAsserted {
            val after = call("GET", "/api/v1/announcements/$announcementId", ownerToken).json
            val confirmed = after["entries"].map { it["clubMemberId"].asText() }
            assertEquals(10, after["confirmedCount"].asInt())
            assertEquals(0, after["waitlistCount"].asInt())
            assertTrue(firstWaitlisted in confirmed)
            assertFalse(leaverMemberId in confirmed)
        }
        assertEquals(403, call("GET", "/api/v1/clubs/$clubId", leaverToken).status)
    }

    @Test
    fun `guests, schedule exceptions, absences, close and draw times and manual score (spec 008)`() {
        val (clubId, code, ownerToken) = newClub("Parity FC")
        val (_, playerToken) = newUser()
        call("POST", "/api/v1/clubs/join", playerToken, """{"invitationCode":"$code"}""")

        val badSchedule = call(
            "POST", "/api/v1/clubs/$clubId/schedule", ownerToken,
            """{"matchDayOfWeek":"THURSDAY","matchTime":"20:00:00","format":"FIVE_A_SIDE","closeDaysBefore":1,"closeTime":"22:00:00","drawDaysBefore":2,"drawTime":"12:00:00"}"""
        )
        assertEquals(400, badSchedule.status, badSchedule.body)
        val schedule = call(
            "POST", "/api/v1/clubs/$clubId/schedule", ownerToken,
            """{"matchDayOfWeek":"THURSDAY","matchTime":"20:00:00","format":"FIVE_A_SIDE","closeDaysBefore":1,"closeTime":"21:00:00","drawDaysBefore":0,"drawTime":"12:00:00"}"""
        )
        assertEquals(201, schedule.status, schedule.body)
        assertEquals("21:00:00", schedule.json["closeTime"].asText())
        assertEquals(0, schedule.json["drawDaysBefore"].asInt())

        val announcement = call("GET", "/api/v1/clubs/$clubId/announcements/current", playerToken).json["announcement"]
        val announcementId = announcement["id"].asText()
        assertTrue(announcement["drawAt"].asText() > announcement["closesAt"].asText(), announcement.toString())

        val withGuest = call("POST", "/api/v1/announcements/$announcementId/guests", playerToken, """{"name":"Cousin","position":"GOALKEEPER"}""")
        assertEquals(200, withGuest.status, withGuest.body)
        val guest = withGuest.json["entries"][0]
        assertEquals("GUEST", guest["participantType"].asText())
        assertTrue(guest["clubMemberId"].isNull)
        assertEquals("Cousin", guest["guestName"].asText())
        assertEquals(400, call("POST", "/api/v1/announcements/$announcementId/guests", playerToken, """{"name":" "}""").status)
        call("POST", "/api/v1/announcements/$announcementId/guests", playerToken, """{"name":"Friend"}""")
        assertEquals(409, call("POST", "/api/v1/announcements/$announcementId/guests", playerToken, """{"name":"Third"}""").status)
        assertEquals(200, call("DELETE", "/api/v1/announcements/$announcementId/guests/${guest["id"].asText()}", ownerToken).status)
        assertEquals(404, call("DELETE", "/api/v1/announcements/$announcementId/guests/${UUID.randomUUID()}", ownerToken).status)

        val match = call("GET", "/api/v1/clubs/$clubId/matches?status=SCHEDULED", ownerToken).json[0]
        val matchId = match["id"].asText()
        val weekAfter = java.time.LocalDate.parse(match["scheduleDate"].asText()).plusWeeks(1)
        assertEquals(403, call("PUT", "/api/v1/matches/$matchId/score", playerToken, """{"teamAScore":3,"teamBScore":2}""").status)
        val scored = call("PUT", "/api/v1/matches/$matchId/score", ownerToken, """{"teamAScore":3,"teamBScore":2}""")
        assertEquals(200, scored.status, scored.body)
        assertTrue(scored.json["isManualScore"].asBoolean())
        assertEquals(3, scored.json["teamAScore"].asInt())
        assertEquals(1, scored.json["enrolledGuests"].size())
        assertFalse(call("DELETE", "/api/v1/matches/$matchId/score", ownerToken).json["isManualScore"].asBoolean())

        val exception = call(
            "POST", "/api/v1/clubs/$clubId/schedule/exceptions", ownerToken,
            """{"date":"$weekAfter","type":"CANCELLED","reason":"Holidays"}"""
        )
        assertEquals(201, exception.status, exception.body)
        assertEquals(403, call("POST", "/api/v1/clubs/$clubId/schedule/exceptions", playerToken, """{"date":"$weekAfter","type":"CANCELLED"}""").status)
        assertEquals(409, call("POST", "/api/v1/clubs/$clubId/schedule/exceptions", ownerToken, """{"date":"$weekAfter","type":"CANCELLED"}""").status)
        assertEquals(1, call("GET", "/api/v1/clubs/$clubId/schedule/exceptions", playerToken).json.size())
        assertEquals(204, call("DELETE", "/api/v1/clubs/$clubId/schedule/exceptions/${exception.json["id"].asText()}", ownerToken).status)

        val absence = call(
            "POST", "/api/v1/clubs/$clubId/members/me/absences", playerToken,
            """{"fromDate":"$weekAfter","toDate":"${weekAfter.plusDays(3)}","reason":"Trip"}"""
        )
        assertEquals(201, absence.status, absence.body)
        assertEquals(400, call("POST", "/api/v1/clubs/$clubId/members/me/absences", playerToken, """{"fromDate":"$weekAfter","toDate":"${weekAfter.minusDays(1)}"}""").status)
        assertEquals(1, call("GET", "/api/v1/clubs/$clubId/absences?from=$weekAfter&to=$weekAfter", ownerToken).json.size())
        assertEquals(404, call("DELETE", "/api/v1/clubs/$clubId/members/me/absences/${absence.json["id"].asText()}", ownerToken).status)
        assertEquals(204, call("DELETE", "/api/v1/clubs/$clubId/members/me/absences/${absence.json["id"].asText()}", playerToken).status)
    }

    @Test
    fun `member management endpoints`() {
        val (_, ownerToken) = newUser()
        val (playerId, playerToken) = newUser()
        val club = call("POST", "/api/v1/clubs", ownerToken, """{"name":"Roles FC"}""").json
        val clubId = club["id"].asText()
        call("POST", "/api/v1/clubs/join", playerToken, """{"invitationCode":"${club["invitationCode"].asText()}"}""")
        val playerMemberId = call("GET", "/api/v1/clubs/$clubId/members", ownerToken).json
            .single { it["userId"].asText() == playerId.toString() }["id"].asText()

        assertEquals(409, call("DELETE", "/api/v1/clubs/$clubId/members/me", ownerToken).status)
        assertEquals(403, call("PATCH", "/api/v1/clubs/$clubId/members/$playerMemberId/role", playerToken, """{"role":"ADMIN"}""").status)

        val promoted = call("PATCH", "/api/v1/clubs/$clubId/members/$playerMemberId/role", ownerToken, """{"role":"CAPTAIN"}""")
        assertEquals(200, promoted.status, promoted.body)
        assertEquals("CAPTAIN", promoted.json["role"].asText())

        val edited = call("PATCH", "/api/v1/clubs/$clubId", ownerToken, """{"name":"Renamed FC","maxMembers":1}""")
        assertEquals(400, edited.status)

        val transferred = call("POST", "/api/v1/clubs/$clubId/transfer-ownership", ownerToken, """{"memberId":"$playerMemberId"}""")
        assertEquals(200, transferred.status, transferred.body)
        assertEquals(playerId.toString(), transferred.json["ownerId"].asText())

        assertEquals(204, call("DELETE", "/api/v1/clubs/$clubId/members/me", ownerToken).status)
    }

    @Test
    fun `banned users cannot rejoin until the ban is lifted`() {
        val (_, ownerToken) = newUser()
        val (playerId, playerToken) = newUser()
        val club = call("POST", "/api/v1/clubs", ownerToken, """{"name":"Ban FC"}""").json
        val clubId = club["id"].asText()
        val joinBody = """{"invitationCode":"${club["invitationCode"].asText()}"}"""
        call("POST", "/api/v1/clubs/join", playerToken, joinBody)
        val memberId = call("GET", "/api/v1/clubs/$clubId/members", ownerToken).json
            .single { it["userId"].asText() == playerId.toString() }["id"].asText()

        assertEquals(204, call("POST", "/api/v1/clubs/$clubId/members/$memberId/ban", ownerToken).status)
        val rejected = call("POST", "/api/v1/clubs/join", playerToken, joinBody)
        assertEquals(403, rejected.status)
        assertEquals("BANNED_FROM_CLUB", rejected.json["code"].asText())
        assertEquals(memberId, call("GET", "/api/v1/clubs/$clubId/bans", ownerToken).json[0]["clubMemberId"].asText())

        assertEquals(204, call("DELETE", "/api/v1/clubs/$clubId/members/$memberId/ban", ownerToken).status)
        assertEquals(200, call("POST", "/api/v1/clubs/join", playerToken, joinBody).status)
    }

    @Test
    fun `non members are rejected and previous routes no longer exist`() {
        val (_, ownerToken) = newUser()
        val (_, strangerToken) = newUser()
        val clubId = call("POST", "/api/v1/clubs", ownerToken, """{"name":"Private FC"}""").json["id"].asText()

        assertEquals(403, call("GET", "/api/v1/clubs/$clubId/members", strangerToken).status)
        assertEquals(404, call("GET", "/api/club", ownerToken).status)
        assertEquals(404, call("GET", "/api/matches/club/$clubId", ownerToken).status)
    }

    @Test
    fun `Kotlin request bodies without JsonProperty annotations are deserialized`() {
        // Regression: Spring MVC uses Jackson 3, which needs its own Kotlin module to build data classes.
        val (_, token) = newUser()

        val device = call("POST", "/api/v1/devices", token, """{"token":"device-token-${UUID.randomUUID()}","platform":"ANDROID"}""")
        assertEquals(201, device.status, device.body)

        val refresh = call("POST", "/api/v1/auth/refresh", body = """{"refreshToken":"not-a-valid-token"}""")
        assertTrue(refresh.status in 400..499, "Expected a client error, got ${refresh.status}: ${refresh.body}")
    }

    @Test
    fun `only chat participants can read its messages`() {
        val (aliceId, aliceToken) = newUser()
        val (bobId, _) = newUser()
        val (strangerId, strangerToken) = newUser()
        // Chat participants are created when users verify their email (UserEvent.Verified through RabbitMQ)
        listOf(aliceId, bobId, strangerId).forEach { id ->
            eventPublisher.publish(UserEvent.Verified(userId = id, email = "$id@squadfy.test", username = "u${id.toString().take(8)}"))
        }
        await().atMost(Duration.ofSeconds(15)).untilAsserted {
            assertEquals(3, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM chat_service.chat_participants WHERE user_id IN (?, ?, ?)", Int::class.java, aliceId, bobId, strangerId
            ))
        }

        val chat = call("POST", "/api/v1/chats", aliceToken, """{"otherUserIds":["$bobId"]}""")
        assertEquals(201, chat.status, chat.body)
        val chatId = chat.json["id"].asText()

        // Alice's request fills the shared cache first: the stranger must still be rejected
        assertEquals(200, call("GET", "/api/v1/chats/$chatId/messages", aliceToken).status)
        assertEquals(403, call("GET", "/api/v1/chats/$chatId/messages", strangerToken).status)
        assertEquals(404, call("GET", "/api/v1/chats/$chatId", strangerToken).status)
    }

    @Test
    fun `every REST endpoint lives under api v1`() {
        val allowedOutsideV1 = listOf("/error", "/v3/api-docs", "/swagger-ui")
        val paths = handlerMapping.handlerMethods.keys.flatMap { it.patternValues }

        val outside = paths.filterNot { path -> path.startsWith("/api/v1/") || allowedOutsideV1.any { path.startsWith(it) } }

        assertTrue(outside.isEmpty(), "Endpoints outside /api/v1: $outside")
    }

    @Test
    fun `OpenAPI documents the v1 API with bearer authentication`() {
        val docs = call("GET", "/v3/api-docs")

        assertEquals(200, docs.status)
        val paths = docs.json["paths"].fieldNames().asSequence().toList()
        assertTrue("/api/v1/clubs" in paths)
        assertTrue("/api/v1/clubs/{clubId}/announcements/current" in paths)
        assertTrue(paths.all { it.startsWith("/api/v1/") }, "Unexpected documented paths: $paths")
        assertTrue(docs.json["components"]["securitySchemes"].has("bearerAuth"))
    }
}
