package com.kikepb.squadfy.testing

/** Same major versions as docker-compose.yml. */
object TestContainerImages {
    const val POSTGRES = "postgres:16-alpine"
    const val RABBITMQ = "rabbitmq:3.13-management-alpine"
    const val REDIS = "redis:7-alpine"
}
