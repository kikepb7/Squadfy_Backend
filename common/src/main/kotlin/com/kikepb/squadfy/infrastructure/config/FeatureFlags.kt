package com.kikepb.squadfy.infrastructure.config

import com.kikepb.squadfy.domain.feature.Feature
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Feature flags of `squadfy.features` (spec 011). A flag missing from the configuration takes the
 * default of [Feature]; an unknown key stops the application so typos never go unnoticed.
 */
@Configuration
@ConfigurationProperties(prefix = "squadfy")
class FeatureFlags {

    var features: Map<String, Boolean> = emptyMap()
        set(value) {
            val unknown = value.keys.filter { Feature.fromKey(it) == null }
            require(unknown.isEmpty()) {
                "Unknown feature flags in squadfy.features: $unknown. Known flags: ${Feature.entries.map { it.key }}"
            }
            field = value
        }

    fun isEnabled(feature: Feature): Boolean = features[feature.key] ?: feature.enabledByDefault

    /** Every flag with its effective value, for clients (`GET /api/v1/features`). */
    fun snapshot(): Map<String, Boolean> = Feature.entries.associate { it.key to isEnabled(it) }
}
