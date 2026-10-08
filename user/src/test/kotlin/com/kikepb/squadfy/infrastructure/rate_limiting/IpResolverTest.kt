package com.kikepb.squadfy.infrastructure.rate_limiting

import com.kikepb.squadfy.infrastructure.config.NginxConfig
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Spec 013 CA-2 / CA-3: client IP behind a trusted proxy. */
class IpResolverTest {

    private fun resolver(header: String = "X-Real-IP", trusted: List<String> = listOf("10.0.0.0/8"), requireProxy: Boolean = true) =
        IpResolver(NginxConfig(trustedIps = trusted, requireProxy = requireProxy, clientIpHeader = header))

    private fun request(remote: String, header: String, vararg values: String) =
        MockHttpServletRequest().apply {
            remoteAddr = remote
            values.forEach { addHeader(header, it) }
        }

    @Test
    fun `CA-2 the last untrusted entry of X-Forwarded-For wins over a forged first one`() {
        val ip = resolver(header = "X-Forwarded-For")
            .getClientIp(request("10.1.2.3", "X-Forwarded-For", "6.6.6.6, 203.0.113.9"))
        assertEquals("203.0.113.9", ip)
    }

    @Test
    fun `CA-2 trusted proxies at the end of the chain are skipped`() {
        val ip = resolver(header = "X-Forwarded-For")
            .getClientIp(request("10.1.2.3", "X-Forwarded-For", "203.0.113.9, 10.9.9.9"))
        assertEquals("203.0.113.9", ip)
    }

    @Test
    fun `CA-2 a direct connection or a missing header fails when a proxy is required`() {
        val resolver = resolver(header = "X-Forwarded-For")
        assertFailsWith<SecurityException> { resolver.getClientIp(request("198.51.100.7", "X-Forwarded-For", "1.1.1.1")) }
        assertFailsWith<SecurityException> { resolver.getClientIp(request("10.1.2.3", "X-Forwarded-For")) }
    }

    @Test
    fun `CA-3 X-Real-IP keeps working by default`() {
        val ip = resolver().getClientIp(request("10.1.2.3", "X-Real-IP", "203.0.113.9"))
        assertEquals("203.0.113.9", ip)
    }

    @Test
    fun `without required proxy the remote address is the fallback`() {
        val ip = resolver(requireProxy = false).getClientIp(request("198.51.100.7", "X-Real-IP", "1.1.1.1"))
        assertEquals("198.51.100.7", ip)
    }
}
