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
import kotlin.test.assertTrue

/** Chat WebSocket protocol: bearer handshake and error envelope for malformed messages. */
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
