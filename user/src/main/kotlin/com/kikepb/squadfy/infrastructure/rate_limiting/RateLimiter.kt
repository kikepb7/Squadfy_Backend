package com.kikepb.squadfy.infrastructure.rate_limiting

import com.kikepb.squadfy.domain.exception.RateLimitException
import com.kikepb.squadfy.domain.feature.Feature
import com.kikepb.squadfy.infrastructure.config.FeatureFlags
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Fixed-window counter in Redis shared by every instance (spec 010 RN-B). Each limit has its own
 * scope (`login:email`, `refresh:user`, `ip:login`…), so limits never share a counter.
 * Switched on and off with the `rate-limit` feature flag (spec 011; on in prod).
 */
@Component
class RateLimiter(
    private val redisTemplate: StringRedisTemplate,
    private val featureFlags: FeatureFlags
) {

    val enabled: Boolean get() = featureFlags.isEnabled(Feature.RATE_LIMIT)

    @Value("classpath:rate_limit.lua")
    lateinit var rateLimitResource: Resource

    private val rateLimitScript by lazy {
        val script = rateLimitResource.inputStream.use { it.readBytes().decodeToString() }
        @Suppress("UNCHECKED_CAST")
        DefaultRedisScript(script, List::class.java as Class<List<Long>>)
    }

    /** Counts one request of [id] in [scope]; throws [RateLimitException] once [maxRequests] per [window] are exceeded. */
    fun check(scope: String, id: String, maxRequests: Int, window: Duration) {
        if (!enabled) return

        val result = redisTemplate.execute(
            rateLimitScript,
            listOf("$KEY_PREFIX:$scope:$id"),
            maxRequests.toString(),
            window.seconds.toString()
        )
        if (result[0] > maxRequests) throw RateLimitException(resetInSeconds = result[1].coerceAtLeast(1))
    }

    private companion object {
        const val KEY_PREFIX = "rate_limit"
    }
}
