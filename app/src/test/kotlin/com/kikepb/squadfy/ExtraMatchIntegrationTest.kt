package com.kikepb.squadfy

import com.kikepb.squadfy.testing.InfrastructureTestContainersConfiguration
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.context.annotation.Import
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals

/**
 * Regression: POST /clubs/{id}/matches returned 500 because optional arguments of
 * MatchPlanningService.createMatchWithAnnouncement were default expressions evaluated on the Spring proxy.
 */
@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    properties = ["jwt.secret=c3F1YWRmeS10ZXN0LXNlY3JldC1zcXVhZGZ5LXRlc3Qtc2VjcmV0LTEyMzQ1Ng==", "firebase.enabled=false"]
)
@Import(InfrastructureTestContainersConfiguration::class)
class ExtraMatchIntegrationTest : ApiIntegrationTestSupport() {

    private fun inMinutes(minutes: Long) = Instant.now().plus(minutes, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS)

    @Test
    fun `extra match without schedule uses the default duration and opens its announcement`() {
        val (clubId, _, token) = newClub()

        val match = call("POST", "/api/v1/clubs/$clubId/matches", token, """{"scheduledAt":"${inMinutes(15)}"}""")

        assertEquals(201, match.status, match.body)
        assertEquals(60, match.json["durationMinutes"].asInt())
        val announcement = call("GET", "/api/v1/matches/${match.json["id"].asText()}/announcement", token)
        assertEquals(200, announcement.status, announcement.body)
    }

    @Test
    fun `extra match of a club with schedule takes its duration when none is given`() {
        val (clubId, _, token) = newClub()
        val schedule = call(
            "POST", "/api/v1/clubs/$clubId/schedule", token,
            """{"matchDayOfWeek":"THURSDAY","matchTime":"20:00:00","timeZone":"Europe/Madrid","format":"SEVEN_A_SIDE","matchDurationMinutes":75}"""
        )
        assertEquals(201, schedule.status, schedule.body)

        val match = call("POST", "/api/v1/clubs/$clubId/matches", token, """{"scheduledAt":"${inMinutes(30)}"}""")

        assertEquals(201, match.status, match.body)
        assertEquals(75, match.json["durationMinutes"].asInt())
    }
}
