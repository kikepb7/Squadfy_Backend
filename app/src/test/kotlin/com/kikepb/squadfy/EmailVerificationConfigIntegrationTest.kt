package com.kikepb.squadfy

import com.fasterxml.jackson.databind.ObjectMapper
import com.kikepb.squadfy.testing.InfrastructureTestContainersConfiguration
import com.kikepb.squadfy.testing.InfrastructureTestContainersConfiguration.Companion.MAILPIT_API_PORT
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.context.annotation.Import
import org.testcontainers.containers.GenericContainer
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Specs 009 and 011: the `email-verification` feature flag in register and login. */
abstract class EmailVerificationConfigSupport : ApiIntegrationTestSupport() {

    protected fun newEmail(prefix: String) = "$prefix${UUID.randomUUID().toString().take(8)}@squadfy.test"

    protected fun register(email: String) = call(
        "POST", "/api/v1/auth/register",
        body = """{"email":"$email","username":"${email.substringBefore('@')}","password":"Password123!"}"""
    )

    protected fun login(email: String) = call("POST", "/api/v1/auth/login", body = """{"email":"$email","password":"Password123!"}""")
}

@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    properties = ["jwt.secret=c3F1YWRmeS10ZXN0LXNlY3JldC1zcXVhZGZ5LXRlc3Qtc2VjcmV0LTEyMzQ1Ng==", "firebase.enabled=false"]
)
@Import(InfrastructureTestContainersConfiguration::class)
class EmailVerificationDefaultIntegrationTest : EmailVerificationConfigSupport() {

    @Test
    fun `CA-1 by default a new user is verified and can log in at once`() {
        val email = newEmail("d")
        val registered = register(email)
        assertEquals(true, registered.json["hasVerifiedEmail"].asBoolean(), registered.body)

        val login = login(email)
        assertEquals(200, login.status, login.body)
        assertTrue(login.json["accessToken"].asText().isNotBlank())
    }

    @Test
    fun `users registered unverified before can log in while the flag is off`() {
        val email = newEmail("p")
        register(email)
        jdbcTemplate.update("UPDATE user_service.users SET has_verified_email = false WHERE email = ?", email)

        assertEquals(200, login(email).status)
    }

    @Test
    fun `CA-3 the app reads the feature flags without a session`() {
        val features = call("GET", "/api/v1/features")

        assertEquals(200, features.status, features.body)
        assertEquals(false, features.json["email-verification"].asBoolean())
        assertEquals(false, features.json["rate-limit"].asBoolean())
    }
}

@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    properties = [
        "jwt.secret=c3F1YWRmeS10ZXN0LXNlY3JldC1zcXVhZGZ5LXRlc3Qtc2VjcmV0LTEyMzQ1Ng==",
        "firebase.enabled=false",
        "squadfy.features.email-verification=true"
    ]
)
@Import(InfrastructureTestContainersConfiguration::class)
class EmailVerificationEnabledIntegrationTest : EmailVerificationConfigSupport() {

    @Autowired lateinit var mailpitContainer: GenericContainer<*>

    private val http = HttpClient.newHttpClient()

    private fun mailpit(path: String): String {
        val uri = URI.create("http://${mailpitContainer.host}:${mailpitContainer.getMappedPort(MAILPIT_API_PORT)}$path")
        return http.send(HttpRequest.newBuilder(uri).build(), HttpResponse.BodyHandlers.ofString()).body()
    }

    @Test
    fun `CA-2 and CA-5 with the flag on a user verifies the email through the page of the email link`() {
        val email = newEmail("v")
        val registered = register(email)
        assertEquals(false, registered.json["hasVerifiedEmail"].asBoolean(), registered.body)
        val blocked = login(email)
        assertEquals(403, blocked.status)
        assertEquals("EMAIL_NOT_VERIFIED", blocked.json["code"].asText())
        assertEquals(true, call("GET", "/api/v1/features").json["email-verification"].asBoolean())

        val messageId = await().atMost(Duration.ofSeconds(20)).until(
            { ObjectMapper().readTree(mailpit("/api/v1/search?query=to:$email"))["messages"].firstOrNull()?.get("ID")?.asText() },
            { it != null }
        )
        val html = ObjectMapper().readTree(mailpit("/api/v1/message/$messageId"))["HTML"].asText()
        val token = assertNotNull(Regex("""/account/verify-email\?token=([\w-]+)""").find(html), html).groupValues[1]

        val page = call("GET", "/account/verify-email?token=$token")
        assertEquals(200, page.status)
        assertTrue("/api/v1/auth/verify?token=" in page.body)

        assertEquals(200, call("GET", "/api/v1/auth/verify?token=$token").status)
        assertEquals(200, login(email).status)
    }
}
