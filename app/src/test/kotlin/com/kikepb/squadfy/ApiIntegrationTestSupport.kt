package com.kikepb.squadfy

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.kikepb.squadfy.service.JwtService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.jdbc.core.JdbcTemplate
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.UUID

/** HTTP helpers for `@SpringBootTest(webEnvironment = RANDOM_PORT)` API tests. */
abstract class ApiIntegrationTestSupport {

    @LocalServerPort private var port: Int = 0
    @Autowired lateinit var jwtService: JwtService
    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    private val http = HttpClient.newHttpClient()

    protected data class ApiResponse(val status: Int, val body: String) {
        val json: JsonNode get() = ObjectMapper().readTree(body)
    }

    protected fun call(method: String, path: String, token: String? = null, body: String? = null): ApiResponse {
        val request = HttpRequest.newBuilder(URI.create("http://localhost:$port$path"))
            .method(method, body?.let { HttpRequest.BodyPublishers.ofString(it) } ?: HttpRequest.BodyPublishers.noBody())
            .header("Content-Type", "application/json")
            .apply { token?.let { header("Authorization", "Bearer $it") } }
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        return ApiResponse(response.statusCode(), response.body())
    }

    /** A verified user inserted directly in the database, with a valid access token. */
    protected fun newUser(): Pair<UUID, String> {
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

    /** Creates a club owned by a new user; returns (clubId, invitationCode, ownerToken). */
    protected fun newClub(name: String = "Test FC"): Triple<String, String, String> {
        val (_, ownerToken) = newUser()
        val club = call("POST", "/api/v1/clubs", ownerToken, """{"name":"$name"}""").json
        return Triple(club["id"].asText(), club["invitationCode"].asText(), ownerToken)
    }
}
