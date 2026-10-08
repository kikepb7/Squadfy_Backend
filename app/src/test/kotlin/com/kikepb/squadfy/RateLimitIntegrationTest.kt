package com.kikepb.squadfy

import com.kikepb.squadfy.api.config.AuthRateLimits
import com.kikepb.squadfy.testing.InfrastructureTestContainersConfiguration
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Spec 010 BE-GAP-2: auth limits are per account, so players sharing an IP do not block each other. */
@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    properties = [
        "jwt.secret=c3F1YWRmeS10ZXN0LXNlY3JldC1zcXVhZGZ5LXRlc3Qtc2VjcmV0LTEyMzQ1Ng==",
        "firebase.enabled=false",
        "squadfy.features.rate-limit=true",
        "nginx.require-proxy=false"
    ]
)
@Import(InfrastructureTestContainersConfiguration::class)
class RateLimitIntegrationTest : ApiIntegrationTestSupport() {

    private fun login(email: String) = call("POST", "/api/v1/auth/login", body = """{"email":"$email","password":"$TEST_PASSWORD"}""")
    private fun refresh(token: String) = call("POST", "/api/v1/auth/refresh", body = """{"refreshToken":"$token"}""")

    @Test
    fun `CA-7 a dozen players on the same wifi can all log in and refresh`() {
        val players = List(12) { registerUser() }

        players.forEach { player ->
            assertEquals(200, login(player.email).status)
            assertEquals(200, refresh(player.refreshToken).status)
        }
    }

    @Test
    fun `CA-8 one account over its limit gets 429 with Retry-After while others go on`() {
        val busy = registerUser()
        val other = registerUser()

        var token = busy.refreshToken
        repeat(AuthRateLimits.REFRESH_PER_USER) {
            val response = refresh(token)
            assertEquals(200, response.status, response.body)
            token = response.json["refreshToken"].asText()
        }
        val limited = refresh(token)
        assertEquals(429, limited.status)
        assertEquals("RATE_LIMIT_EXCEEDED", limited.json["code"].asText())
        assertTrue(assertNotNull(limited.header("Retry-After")).toLong() in 1..3600)
        assertEquals(200, refresh(other.refreshToken).status)

        // registerUser already logged in once with each email
        repeat(AuthRateLimits.LOGIN_PER_EMAIL - 1) { assertEquals(200, login(busy.email).status) }
        assertEquals(429, login(busy.email).status)
        assertEquals(200, login(other.email).status)

        // Invalid refresh tokens are counted per IP
        repeat(AuthRateLimits.INVALID_REFRESH_PER_IP) { assertEquals(401, refresh("not-a-token").status) }
        assertEquals(429, refresh("not-a-token").status)
        assertEquals(200, login(other.email).status)
    }
}
