package com.kikepb.squadfy.infrastructure.storage

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class SupabaseRestClientConfig(
    @param:Value("\${supabase.project-url}") private val supabaseProjectUrl: String,
    @param:Value("\${supabase.service-key}") private val supabaseServiceKey: String
) {

    @Bean
    fun supabaseRestClient(): RestClient {
        return RestClient.builder()
            .baseUrl(supabaseProjectUrl)
            .defaultHeader("Authorization", "Bearer $supabaseServiceKey")
            .build()
    }
}
