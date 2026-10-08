package com.kikepb.squadfy

import com.fasterxml.jackson.databind.ObjectMapper
import com.kikepb.squadfy.testing.InfrastructureTestContainersConfiguration
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** WebSocket protocol: bearer handshake, error envelope for malformed messages and live club updates (spec 012). */
@SpringBootTest(
    webEnvironment = RANDOM_PORT,
    properties = [
        "jwt.secret=c3F1YWRmeS10ZXN0LXNlY3JldC1zcXVhZGZ5LXRlc3Qtc2VjcmV0LTEyMzQ1Ng==",
        "firebase.enabled=false"
    ]
)
@Import(InfrastructureTestContainersConfiguration::class)
class ChatWebSocketIntegrationTest : ApiIntegrationTestSupport() {

    @LocalServerPort private var serverPort: Int = 0

    private class CollectingListener : WebSocket.Listener {
        val messages = LinkedBlockingQueue<String>()
        private val buffer = StringBuilder()

        override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
            buffer.append(data)
            if (last) {
                messages.add(buffer.toString())
                buffer.clear()
            }
            webSocket.request(1)
            return CompletableFuture.completedFuture(null)
        }
    }

    @Test
    fun `a malformed message gets an ERROR envelope and the connection stays open`() {
        val (_, token) = newUser()
        val listener = CollectingListener()
        val socket = HttpClient.newHttpClient().newWebSocketBuilder()
            .header("Authorization", "Bearer $token")
            .buildAsync(URI.create("ws://localhost:$serverPort/ws/chat"), listener)
            .get(10, TimeUnit.SECONDS)

        socket.sendText("""{"type":"NEW_MESSAGE","payload":"not-json"}""", true).get(5, TimeUnit.SECONDS)

        val reply = assertNotNull(listener.messages.poll(10, TimeUnit.SECONDS), "No reply from the server")
        val envelope = ObjectMapper().readTree(reply)
        assertEquals("ERROR", envelope["type"].asText())
        assertEquals("INVALID_JSON", ObjectMapper().readTree(envelope["payload"].asText())["code"].asText())
        assertTrue(!socket.isOutputClosed && !socket.isInputClosed)
        socket.sendClose(WebSocket.NORMAL_CLOSURE, "bye").get(5, TimeUnit.SECONDS)
    }

    private fun connect(token: String, listener: CollectingListener): WebSocket =
        HttpClient.newHttpClient().newWebSocketBuilder()
            .header("Authorization", "Bearer $token")
            .buildAsync(URI.create("ws://localhost:$serverPort/ws/chat"), listener)
            .get(10, TimeUnit.SECONDS)

    @Test
    fun `CA-1 connected members are told once to reload a match when someone enrolls`() {
        val (clubId, code, ownerToken) = newClub("Live FC")
        val (_, playerToken) = newUser()
        val (_, outsiderToken) = newUser()
        call("POST", "/api/v1/clubs/join", playerToken, """{"invitationCode":"$code"}""")
        call("POST", "/api/v1/clubs/$clubId/schedule", ownerToken, """{"matchDayOfWeek":"THURSDAY","matchTime":"20:00:00","format":"FIVE_A_SIDE"}""")
        val current = call("GET", "/api/v1/clubs/$clubId/announcements/current", ownerToken).json
        val announcementId = current["announcement"]["id"].asText()
        val matchId = current["announcement"]["matchId"].asText()

        val owner = CollectingListener()
        val outsider = CollectingListener()
        val ownerSocket = connect(ownerToken, owner)
        val outsiderSocket = connect(outsiderToken, outsider)

        assertEquals(200, call("POST", "/api/v1/announcements/$announcementId/enrollment", playerToken).status)

        val received = assertNotNull(owner.messages.poll(10, TimeUnit.SECONDS), "No live update for the member")
        val envelope = ObjectMapper().readTree(received)
        assertEquals("CLUB_DATA_CHANGED", envelope["type"].asText())
        val payload = ObjectMapper().readTree(envelope["payload"].asText())
        assertEquals(clubId, payload["clubId"].asText())
        assertEquals("MATCH", payload["scope"].asText())
        assertEquals(matchId, payload["matchId"].asText())
        assertNull(owner.messages.poll(1, TimeUnit.SECONDS), "One operation must produce a single notice")
        assertNull(outsider.messages.poll(1, TimeUnit.SECONDS), "Users of other clubs are not notified")

        ownerSocket.sendClose(WebSocket.NORMAL_CLOSURE, "bye").get(5, TimeUnit.SECONDS)
        outsiderSocket.sendClose(WebSocket.NORMAL_CLOSURE, "bye").get(5, TimeUnit.SECONDS)
    }

    @Test
    fun `the handshake requires a valid access token`() {
        val result = runCatching {
            HttpClient.newHttpClient().newWebSocketBuilder()
                .buildAsync(URI.create("ws://localhost:$serverPort/ws/chat"), CollectingListener())
                .get(10, TimeUnit.SECONDS)
        }
        assertTrue(result.isFailure, "Handshake without token must be rejected")
    }
}
