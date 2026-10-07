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

    /** Public account deletion page for the stores (spec 010 RN-A8). */
    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addViewController(ACCOUNT_DELETION_PAGE).setViewName("forward:$ACCOUNT_DELETION_PAGE.html")
    }

    companion object {
        const val ACCOUNT_DELETION_PAGE = "/account/delete"
    }
}