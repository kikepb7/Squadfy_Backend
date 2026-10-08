package com.kikepb.squadfy.domain.feature

/**
 * Functions that can be switched on or off per environment (spec 011). Each one is configured as
 * `squadfy.features.<key>` or with the environment variable `FEATURE_<KEY>`.
 * To add a flag: add an entry here, declare it in `application.yml` and check it with `FeatureFlags`.
 */
enum class Feature(val key: String, val enabledByDefault: Boolean) {
    /** Users must verify their email before logging in (spec 009). */
    EMAIL_VERIFICATION(key = "email-verification", enabledByDefault = false),

    /** Per-account and per-IP limits of the auth endpoints (spec 010). */
    RATE_LIMIT(key = "rate-limit", enabledByDefault = false);

    companion object {
        fun fromKey(key: String): Feature? = entries.firstOrNull { it.key == key }
    }
}
