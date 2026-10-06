package com.kikepb.squadfy

import com.kikepb.squadfy.domain.events.match.MatchEvent
import com.kikepb.squadfy.domain.model.PushNotificationModel
import com.kikepb.squadfy.infrastructure.message_queue.EventPublisher
import com.kikepb.squadfy.infrastructure.push_notification.FirebasePushNotificationService
import com.kikepb.squadfy.testing.InfrastructureTestContainersConfiguration
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mockingDetails
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Spec 005 end to end: match events travel through RabbitMQ to the notification module, which
 * skips users who muted the club (except waitlist promotions) and hands the push to Firebase.
 */
@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    properties = [
        "jwt.secret=c3F1YWRmeS10ZXN0LXNlY3JldC1zcXVhZGZ5LXRlc3Qtc2VjcmV0LTEyMzQ1Ng==",
        "firebase.enabled=false"
    ]
)
@Import(InfrastructureTestContainersConfiguration::class)
class MatchNotificationsIntegrationTest : ApiIntegrationTestSupport() {

    @Autowired lateinit var eventPublisher: EventPublisher
    @MockitoSpyBean lateinit var firebase: FirebasePushNotificationService

    private fun sentPushes(): List<PushNotificationModel> =
        mockingDetails(firebase).invocations
            .filter { it.method.name == "sendNotification" }
            .map { it.arguments.first() as PushNotificationModel }

    @Test
    fun `muted members do not receive club pushes but always receive their waitlist promotion`() {
        val (clubId, code, _) = newClub(name = "Push FC")
        val (activeUser, activeToken) = newUser()
        val (mutedUser, mutedToken) = newUser()
        listOf(activeToken, mutedToken).forEachIndexed { index, token ->
            assertEquals(200, call("POST", "/api/v1/clubs/join", token, """{"invitationCode":"$code"}""").status)
            assertEquals(201, call("POST", "/api/v1/devices", token, """{"token":"device-$index-$clubId","platform":"ANDROID"}""").status)
        }

        val muted = call("PUT", "/api/v1/clubs/$clubId/notification-settings", mutedToken, """{"muted":true}""")
        assertEquals(200, muted.status, muted.body)
        assertTrue(call("GET", "/api/v1/clubs/$clubId/notification-settings", mutedToken).json["muted"].asBoolean())
        assertEquals(false, call("GET", "/api/v1/clubs/$clubId/notification-settings", activeToken).json["muted"].asBoolean())

        val clubUuid = UUID.fromString(clubId)
        val matchId = UUID.randomUUID()
        val kickoff = Instant.now().plus(Duration.ofDays(3))
        eventPublisher.publish(
            MatchEvent.AnnouncementOpened(
                clubId = clubUuid, clubName = "Push FC", matchId = matchId, matchScheduledAt = kickoff,
                timeZone = "Europe/Madrid", announcementId = UUID.randomUUID(), closesAt = kickoff.minus(Duration.ofDays(1)),
                recipientUserIds = listOf(activeUser, mutedUser)
            )
        )
        await().atMost(Duration.ofSeconds(15)).untilAsserted {
            val opened = sentPushes().single { it.title == "Push FC: convocatoria abierta" }
            assertEquals(listOf("device-0-$clubId"), opened.recipients.map { it.token })
        }

        eventPublisher.publish(
            MatchEvent.PromotedFromWaitlist(
                clubId = clubUuid, clubName = "Push FC", matchId = matchId, matchScheduledAt = kickoff,
                timeZone = "Europe/Madrid", announcementId = UUID.randomUUID(), userId = mutedUser
            )
        )
        await().atMost(Duration.ofSeconds(15)).untilAsserted {
            val promoted = sentPushes().single { it.title == "Push FC: ¡tienes plaza!" }
            assertEquals(listOf("device-1-$clubId"), promoted.recipients.map { it.token })
        }
    }

    @Test
    fun `only members can change a club's notification settings`() {
        val (clubId, _, _) = newClub()
        val (_, strangerToken) = newUser()

        assertEquals(403, call("PUT", "/api/v1/clubs/$clubId/notification-settings", strangerToken, """{"muted":true}""").status)
    }
}
