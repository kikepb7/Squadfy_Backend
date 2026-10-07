package com.kikepb.squadfy.api.config

import com.kikepb.squadfy.domain.type.UserId
import com.kikepb.squadfy.infrastructure.rate_limiting.IpResolver
import com.kikepb.squadfy.infrastructure.rate_limiting.RateLimiter
import com.kikepb.squadfy.service.JwtService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Per-account limits of spec 010 RN-B1, so players sharing an IP (the club's wifi) do not block
 * each other. The per-IP safety nets are declared with [IpRateLimit] on the endpoints.
 */
@Component
class AuthRateLimits(
    private val rateLimiter: RateLimiter,
    private val jwtService: JwtService,
    private val ipResolver: IpResolver
) {

    /** By the user of a validly signed refresh token; invalid tokens are counted per IP. */
    fun refresh(refreshToken: String, request: HttpServletRequest) {
        if (jwtService.validateRefreshToken(token = refreshToken)) {
            val userId = jwtService.getUserIdFromToken(token = refreshToken)
            rateLimiter.check(scope = "refresh:user", id = userId.toString(), maxRequests = REFRESH_PER_USER, window = HOUR)
        } else if (rateLimiter.enabled) {
            rateLimiter.check(scope = "refresh:invalid", id = ipResolver.getClientIp(request), maxRequests = INVALID_REFRESH_PER_IP, window = HOUR)
        }
    }

    fun login(email: String) =
        rateLimiter.check(scope = "login:email", id = normalize(email), maxRequests = LOGIN_PER_EMAIL, window = HOUR)

    fun deleteAccount(email: String) =
        rateLimiter.check(scope = "delete:email", id = normalize(email), maxRequests = DELETE_PER_ACCOUNT, window = HOUR)

    fun deleteAccount(userId: UserId) =
        rateLimiter.check(scope = "delete:user", id = userId.toString(), maxRequests = DELETE_PER_ACCOUNT, window = HOUR)

    private fun normalize(email: String) = email.trim().lowercase()

    companion object {
        const val REFRESH_PER_USER = 60
        const val INVALID_REFRESH_PER_IP = 30
        const val LOGIN_PER_EMAIL = 10
        const val DELETE_PER_ACCOUNT = 5

        /** Per-IP safety nets (requests per hour). */
        const val LOGIN_REFRESH_PER_IP = 300
        const val REGISTER_PER_IP = 50
        const val WEB_DELETE_PER_IP = 50

        private val HOUR: Duration = Duration.ofHours(1)
    }
}
