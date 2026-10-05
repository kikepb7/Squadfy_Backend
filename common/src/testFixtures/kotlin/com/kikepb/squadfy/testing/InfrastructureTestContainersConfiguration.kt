package com.kikepb.squadfy.testing

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.testcontainers.containers.GenericContainer
import org.testcontainers.rabbitmq.RabbitMQContainer

/** Everything the full application connects to (PostgreSQL, RabbitMQ, Redis) for `@SpringBootTest`. */
@TestConfiguration(proxyBeanMethods = false)
@Import(PostgresTestContainerConfiguration::class)
class InfrastructureTestContainersConfiguration {

    @Bean
    @ServiceConnection
    fun rabbitMqContainer(): RabbitMQContainer = RabbitMQContainer(TestContainerImages.RABBITMQ)

    @Bean
    @ServiceConnection(name = "redis")
    fun redisContainer(): GenericContainer<*> = GenericContainer(TestContainerImages.REDIS).withExposedPorts(6379)
}
