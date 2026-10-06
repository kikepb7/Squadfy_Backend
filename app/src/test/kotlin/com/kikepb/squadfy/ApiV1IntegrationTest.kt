package com.kikepb.squadfy

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

    @Test
    fun `club, schedule, announcement and ratings flow through v1`() {
        val (_, token) = newUser()

        val me = call("GET", "/api/v1/me", token)
        assertEquals(200, me.status)
        assertTrue(me.json["email"].asText().endsWith("@squadfy.test"))

        val club = call("POST", "/api/v1/clubs", token, """{"name":"Squadfy FC"}""")
        assertEquals(201, club.status, club.body)
        val clubId = club.json["id"].asText()

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
