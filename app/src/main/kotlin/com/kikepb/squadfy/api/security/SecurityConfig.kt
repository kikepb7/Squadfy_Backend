package com.kikepb.squadfy.api.security

import com.kikepb.squadfy.api.config.JwtAuthFilter
import jakarta.servlet.DispatcherType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfig {

    @Bean
    fun filterChain(httpSecurity: HttpSecurity, jwtAuthFilter: JwtAuthFilter): SecurityFilterChain {
        return httpSecurity
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/v1/auth/change-password")
                    .authenticated()
                    .requestMatchers("/", "/api/v1/auth/**")
                    .permitAll()
                    // Spec 010: public account deletion page (its form calls /api/v1/auth/delete-account)
                    .requestMatchers("/account/delete", "/account/delete.html")
                    .permitAll()
                    // API docs: only served when springdoc is enabled (dev profile), otherwise 404
                    .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                    .permitAll()
                    .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info")
                    .permitAll()
                    .dispatcherTypeMatchers(
                        DispatcherType.ERROR,
                        DispatcherType.FORWARD
                    )
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .exceptionHandling { configure ->
                configure
                    .authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            }
            .build()
    }
}