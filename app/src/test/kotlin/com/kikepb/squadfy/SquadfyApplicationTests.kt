package com.kikepb.squadfy

import com.kikepb.squadfy.testing.InfrastructureTestContainersConfiguration
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The whole application starts against throw-away PostgreSQL, RabbitMQ and Redis containers. */
@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    properties = [
        "jwt.secret=c3F1YWRmeS10ZXN0LXNlY3JldC1zcXVhZGZ5LXRlc3Qtc2VjcmV0LTEyMzQ1Ng==",
        "firebase.enabled=false"
    ]
)
@Import(InfrastructureTestContainersConfiguration::class)
class SquadfyApplicationTests {

    @LocalServerPort
    private var port: Int = 0

    private val httpClient = HttpClient.newHttpClient()

    private fun get(path: String): HttpResponse<String> =
        httpClient.send(
            HttpRequest.newBuilder(URI.create("http://localhost:$port$path")).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )

    @Test
    fun `health and probes are public and up`() {
        listOf("/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness").forEach { path ->
            val response = get(path)
            assertEquals(200, response.statusCode(), path)
            assertTrue(response.body().contains("\"UP\""), "$path -> ${response.body()}")
        }
    }

    @Test
    fun `other actuator endpoints are not exposed and the API requires authentication`() {
        // Unauthenticated requests are rejected by security before reaching the (unexposed) endpoint.
        assertEquals(401, get("/actuator/env").statusCode())
        assertEquals(401, get("/api/v1/clubs").statusCode())
    }
}
