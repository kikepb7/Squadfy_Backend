package com.kikepb.squadfy.api.config

import org.springframework.stereotype.Component
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Component
class WebMvcConfig(
    private val ipRateLimitInterceptor: IpRateLimitInterceptor
): WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry
            .addInterceptor(ipRateLimitInterceptor)
            .addPathPatterns("/api/**")
    }

    /** Public pages: account deletion for the stores (spec 010 RN-A8) and email verification (spec 011). */
    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addViewController(ACCOUNT_DELETION_PAGE).setViewName("forward:$ACCOUNT_DELETION_PAGE.html")
        registry.addViewController(EMAIL_VERIFICATION_PAGE).setViewName("forward:$EMAIL_VERIFICATION_PAGE.html")
    }

    companion object {
        const val ACCOUNT_DELETION_PAGE = "/account/delete"

        /** Opened from the verification email (spec 011 RN-B1). */
        const val EMAIL_VERIFICATION_PAGE = "/account/verify-email"
    }
}