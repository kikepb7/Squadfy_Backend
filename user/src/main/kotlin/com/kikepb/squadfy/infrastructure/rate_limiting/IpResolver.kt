package com.kikepb.squadfy.infrastructure.rate_limiting

import com.kikepb.squadfy.infrastructure.config.NginxConfig
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.security.web.util.matcher.IpAddressMatcher
import org.springframework.stereotype.Component
import java.net.Inet4Address
import java.net.Inet6Address

@Component
class IpResolver(
    private val nginxConfig: NginxConfig
) {
    companion object {
        private val PRIVATE_IP_RANGES = listOf(
            "10.0.0.0/8",
            "172.16.0.0/12",
            "192.168.0.0/16",
            "127.0.0.0/8",
            "::1/128",
            "fc00::/7",
            "fe80::/10"
        ).map { IpAddressMatcher(it) }

        private val INVALID_IPS = listOf(
            "unknown",
            "unavailable",
            "0.0.0.0",
            "::"
        )
    }

    private val trustedMatchers: List<IpAddressMatcher> = nginxConfig
        .trustedIps
        .filter { it.isNotBlank() }
        .map { proxy ->
            val cidr = when {
                proxy.contains("/") -> proxy
                proxy.contains(":") -> "$proxy/128"
                else -> "$proxy/32"
            }
            IpAddressMatcher(cidr)
        }

    private val logger = LoggerFactory.getLogger(IpResolver::class.java)

    private fun isPrivateIp(ip: String): Boolean {
        return PRIVATE_IP_RANGES.any{ it.matches(ip) }
    }

    private fun isFromTrustedProxy(ip: String): Boolean {
        return trustedMatchers.any { matcher ->
            matcher.matches(ip)
        }
    }

    /**
     * Walks the header from the right and returns the first valid IP that is not a trusted proxy:
     * entries a client prepends to the header (further left) can never be chosen (spec 013 RN-2).
     */
    private fun extractClientIp(request: HttpServletRequest, proxyIp: String): String? {
        val headerName = nginxConfig.clientIpHeader
        return request.getHeaders(headerName).toList()
            .flatMap { it.split(",") }
            .asReversed()
            .asSequence()
            .mapNotNull { validateAndNormalizeIp(ip = it, headerName = headerName, proxyIp = proxyIp) }
            .firstOrNull { !isFromTrustedProxy(it) }
    }

    private fun validateAndNormalizeIp(ip: String, headerName: String, proxyIp: String): String? {
        val trimmedIp = ip.trim()

        if (trimmedIp.isBlank() || INVALID_IPS.contains(trimmedIp)) {
            logger.debug("Invalid IP in $headerName: $ip from proxy $proxyIp")
            return null
        }

        return try {
            val inetAddr = when {
                trimmedIp.contains(":") -> Inet6Address.getByName(trimmedIp)
                trimmedIp.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")) -> Inet4Address.getByName(trimmedIp)
                else -> {
                    logger.warn("Invalid IP format in $headerName: $trimmedIp from proxy $proxyIp")
                    return null
                }
            }

            if (isPrivateIp(inetAddr.hostAddress)) {
                logger.debug("Private IP in $headerName: $trimmedIp from proxy $proxyIp")
            }

            inetAddr.hostAddress
        } catch (e: Exception) {
            logger.warn("Invalid IP format in $headerName: $trimmedIp from proxy $proxyIp", e)
            null
        }
    }

    fun getClientIp(request: HttpServletRequest): String {
        val remoteAddr = request.remoteAddr

        if (!isFromTrustedProxy(remoteAddr)) {
            if (nginxConfig.requireProxy) {
                logger.warn("Direct connection attempt from $remoteAddr")
                throw SecurityException("Not valid client IP in proxy headers")
            }

            return remoteAddr
        }

        val clientIp = extractClientIp(request = request, proxyIp = remoteAddr)

        if (clientIp == null) {
            logger.warn("No valid client Ip in proxy headers")

            if (nginxConfig.requireProxy) throw SecurityException("No valid client IP in proxy headers")
        }

        return clientIp ?: remoteAddr
    }
}