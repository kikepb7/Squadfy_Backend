package com.kikepb.squadfy.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Functional switches of the backend (`squadfy.app-config`, spec 009).
 * Production pins them in `application-prod.yml`; the other environments read them from environment variables.
 */
@Configuration
@ConfigurationProperties(prefix = "squadfy.app-config")
data class AppConfig(
    var emailVerification: EmailVerification = EmailVerification()
) {
    data class EmailVerification(
        /** RN-1: when false, new users are verified on registration and login does not require it (local/QA tests). */
        var enabled: Boolean = true
    )
}
