package com.kikepb.squadfy.testing

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.postgresql.PostgreSQLContainer

/** PostgreSQL for slice tests: `@Import(PostgresTestContainerConfiguration::class)`. */
@TestConfiguration(proxyBeanMethods = false)
class PostgresTestContainerConfiguration {

    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer = PostgreSQLContainer(TestContainerImages.POSTGRES)
}
