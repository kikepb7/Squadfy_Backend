package com.kikepb.squadfy.api.config

import com.kikepb.squadfy.infrastructure.rate_limiting.IpResolver
import com.kikepb.squadfy.infrastructure.rate_limiting.RateLimiter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import java.time.Duration

/**
 * Per-IP safety net of the endpoints annotated with [IpRateLimit]; each endpoint has its own counter.
 * A [com.kikepb.squadfy.domain.exception.RateLimitException] becomes a 429 in the exception handler.
 */
@Component
class IpRateLimitInterceptor(
    private val rateLimiter: RateLimiter,
    private val ipResolver: IpResolver
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (!rateLimiter.enabled) return true
        val annotation = (handler as? HandlerMethod)?.getMethodAnnotation(IpRateLimit::class.java) ?: return true

        rateLimiter.check(
            scope = "ip:${handler.method.name}",
            id = ipResolver.getClientIp(request),
            maxRequests = annotation.requests,
            window = Duration.of(annotation.duration, annotation.unit.toChronoUnit())
        )
        return true
    }
}
