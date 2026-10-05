package com.kikepb.squadfy

import com.kikepb.squadfy.testing.InfrastructureTestContainersConfiguration
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

/** The whole application starts against throw-away PostgreSQL, RabbitMQ and Redis containers. */
@SpringBootTest(
    properties = [
        "jwt.secret=c3F1YWRmeS10ZXN0LXNlY3JldC1zcXVhZGZ5LXRlc3Qtc2VjcmV0LTEyMzQ1Ng==",
        "firebase.enabled=false"
    ]
)
@Import(InfrastructureTestContainersConfiguration::class)
class SquadfyApplicationTests {

    @Test
    fun contextLoads() {
    }
}
