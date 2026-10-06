package com.kikepb.squadfy

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.kikepb.squadfy.service.JwtService
import com.kikepb.squadfy.testing.InfrastructureTestContainersConfiguration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
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
class ApiV1IntegrationTest {

    @LocalServerPort private var port: Int = 0
    @Autowired lateinit var jwtService: JwtService
    @Autowired lateinit var jdbcTemplate: JdbcTemplate
    @Autowired @Qualifier("requestMappingHandlerMapping") lateinit var handlerMapping: RequestMappingHandlerMapping

    private val http = HttpClient.newHttpClient()
    private val json = ObjectMapper()

    private data class ApiResponse(val status: Int, val body: String) {
        val json: JsonNode get() = ObjectMapper().readTree(body)
    }

    private fun call(method: String, path: String, token: String? = null, body: String? = null): ApiResponse {
        val request = HttpRequest.newBuilder(URI.create("http://localhost:$port$path"))
            .method(method, body?.let { HttpRequest.BodyPublishers.ofString(it) } ?: HttpRequest.BodyPublishers.noBody())
            .header("Content-Type", "application/json")
            .apply { token?.let { header("Authorization", "Bearer $it") } }
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        return ApiResponse(response.statusCode(), response.body())
    }

    private fun newUser(): Pair<UUID, String> {
        val userId = UUID.randomUUID()
        val name = "u${userId.toString().take(8)}"
        jdbcTemplate.update(
            """
            INSERT INTO user_service.users (id, email, username, hashed_password, has_verified_email, is_active, created_at, updated_at)
            VALUES (?, ?, ?, 'not-used', true, true, now(), now())
            """.trimIndent(),
            userId, "$name@squadfy.test", name
        )
        return userId to jwtService.generateAccessToken(userId)
    }

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
