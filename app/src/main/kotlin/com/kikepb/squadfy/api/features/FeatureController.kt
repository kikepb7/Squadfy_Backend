package com.kikepb.squadfy.api.features

import com.kikepb.squadfy.infrastructure.config.FeatureFlags
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/features")
@Tag(name = "Features", description = "Feature flags of this environment (spec 011)")
class FeatureController(
    private val featureFlags: FeatureFlags
) {

    @GetMapping
    @Operation(
        summary = "Feature flags and whether each one is on (public)",
        description = "Lets the app adapt its screens, e.g. skip the 'check your email' step when email-verification is off."
    )
    fun getFeatures(): Map<String, Boolean> = featureFlags.snapshot()
}
