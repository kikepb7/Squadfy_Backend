package com.kikepb.squadfy.api.security

import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Spec 013 CA-4. */
class CorsConfigTest {

    private fun allowedOriginFor(origin: String, configured: List<String>): String? {
        val request = MockHttpServletRequest("OPTIONS", "/api/v1/clubs").apply {
            addHeader("Origin", origin)
            addHeader("Access-Control-Request-Method", "GET")
        }
        val configuration = assertNotNull(CorsConfig(configured).corsConfigurationSource().getCorsConfiguration(request))
        return configuration.checkOrigin(origin)
    }

    @Test
    fun `CA-4 only the configured origins and local patterns are allowed`() {
        val configured = listOf("https://squadfy.app", "http://localhost:*")
        assertEquals("https://squadfy.app", allowedOriginFor("https://squadfy.app", configured))
        assertEquals("http://localhost:5173", allowedOriginFor("http://localhost:5173", configured))
        assertNull(allowedOriginFor("https://evil.example", configured))
    }

    @Test
    fun `CA-4 no configured origin allows no cross-origin request`() {
        assertNull(allowedOriginFor("https://squadfy.app", emptyList()))
    }
}
