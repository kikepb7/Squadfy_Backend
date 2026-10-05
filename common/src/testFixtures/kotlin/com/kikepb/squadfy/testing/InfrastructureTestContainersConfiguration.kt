package com.kikepb.squadfy.testing

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistrar
import org.testcontainers.containers.GenericContainer
import org.testcontainers.rabbitmq.RabbitMQContainer

/**
 * Everything the full application connects to (PostgreSQL, RabbitMQ, Redis, SMTP) for `@SpringBootTest`,
 * so tests never reach services running on the host.
 */
@TestConfiguration(proxyBeanMethods = false)
@Import(PostgresTestContainerConfiguration::class)
class InfrastructureTestContainersConfiguration {

    @Bean
    @ServiceConnection
    fun rabbitMqContainer(): RabbitMQContainer = RabbitMQContainer(TestContainerImages.RABBITMQ)

    @Bean
    @ServiceConnection(name = "redis")
    fun redisContainer(): GenericContainer<*> = GenericContainer(TestContainerImages.REDIS).withExposedPorts(6379)

    @Bean
    fun mailpitContainer(): GenericContainer<*> =
        GenericContainer(TestContainerImages.MAILPIT).withExposedPorts(MAILPIT_SMTP_PORT, MAILPIT_API_PORT)

    @Bean
    fun mailProperties(mailpitContainer: GenericContainer<*>): DynamicPropertyRegistrar =
        DynamicPropertyRegistrar { registry ->
            registry.add("spring.mail.host") { mailpitContainer.host }
            registry.add("spring.mail.port") { mailpitContainer.getMappedPort(MAILPIT_SMTP_PORT) }
        }

    companion object {
        const val MAILPIT_SMTP_PORT = 1025
        const val MAILPIT_API_PORT = 8025
    }
}
