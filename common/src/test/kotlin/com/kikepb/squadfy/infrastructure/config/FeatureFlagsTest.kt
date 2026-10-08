package com.kikepb.squadfy.infrastructure.config

import com.kikepb.squadfy.domain.feature.Feature
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeatureFlagsTest {

    @Test
    fun `flags take their default unless configured`() {
        val flags = FeatureFlags().apply { features = mapOf("rate-limit" to true) }

        assertTrue(flags.isEnabled(Feature.RATE_LIMIT))
        assertFalse(flags.isEnabled(Feature.EMAIL_VERIFICATION))
        assertEquals(mapOf("email-verification" to false, "rate-limit" to true), flags.snapshot())
    }

    @Test
    fun `CA-4 an unknown flag is rejected naming it`() {
        val error = assertFailsWith<IllegalArgumentException> {
            FeatureFlags().apply { features = mapOf("email-verifcation" to true) }
        }
        assertTrue("email-verifcation" in error.message.orEmpty())
    }
}
