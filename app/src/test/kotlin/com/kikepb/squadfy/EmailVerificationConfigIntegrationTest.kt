package com.kikepb.squadfy

import com.kikepb.squadfy.testing.InfrastructureTestContainersConfiguration
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.context.annotation.Import
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Spec 009: register and log in through the API with the given `squadfy.app-config`. */
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
class EmailVerificationEnabledIntegrationTest : EmailVerificationConfigSupport() {

    @Test
    fun `CA-1 by default a new user cannot log in until the email is verified`() {
        val email = newEmail("v")
        val registered = register(email)
        assertEquals(200, registered.status, registered.body)
        assertEquals(false, registered.json["hasVerifiedEmail"].asBoolean())

        val login = login(email)
        assertEquals(403, login.status)
        assertEquals("EMAIL_NOT_VERIFIED", login.json["code"].asText())
    }
}

@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    properties = [
        "jwt.secret=c3F1YWRmeS10ZXN0LXNlY3JldC1zcXVhZGZ5LXRlc3Qtc2VjcmV0LTEyMzQ1Ng==",
        "firebase.enabled=false",
        "squadfy.app-config.email-verification.enabled=false"
    ]
)
@Import(InfrastructureTestContainersConfiguration::class)
class EmailVerificationDisabledIntegrationTest : EmailVerificationConfigSupport() {

    @Test
    fun `CA-2 with verification off a new user is verified and can log in at once`() {
        val email = newEmail("d")
        val registered = register(email)
        assertEquals(true, registered.json["hasVerifiedEmail"].asBoolean(), registered.body)

        val login = login(email)
        assertEquals(200, login.status, login.body)
        assertTrue(login.json["accessToken"].asText().isNotBlank())
    }

    @Test
    fun `CA-3 with verification off users registered before can log in without verifying`() {
        val email = newEmail("p")
        register(email)
        jdbcTemplate.update("UPDATE user_service.users SET has_verified_email = false WHERE email = ?", email)

        assertEquals(200, login(email).status)
    }
}
