package com.kikepb.squadfy.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "nginx")
data class NginxConfig(
    var trustedIps: List<String> = emptyList(),
    var requireProxy: Boolean = true,
    /** Header carrying the client IP: X-Real-IP behind nginx, X-Forwarded-For behind Render (spec 013). */
    var clientIpHeader: String = "X-Real-IP"
)
