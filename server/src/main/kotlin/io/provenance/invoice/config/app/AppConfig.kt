package io.provenance.invoice.config.app

import com.fasterxml.jackson.databind.ObjectMapper
import io.provenance.invoice.config.ConfigurationUtil
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
@EnableConfigurationProperties(value = [ServiceProperties::class])
class AppConfig {
    @Primary
    @Bean
    fun objectMapper(): ObjectMapper = ConfigurationUtil.DEFAULT_OBJECT_MAPPER
}
