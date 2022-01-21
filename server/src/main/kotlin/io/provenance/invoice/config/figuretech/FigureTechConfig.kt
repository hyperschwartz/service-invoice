package io.provenance.invoice.config.figuretech

import com.fasterxml.jackson.databind.ObjectMapper
import io.provenance.invoice.clients.OnboardingApiClient
import io.provenance.invoice.config.ConfigurationUtil
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(value = [FigureTechProperties::class])
class FigureTechConfig {
    @Bean
    fun onboardingApiClient(
        mapper: ObjectMapper,
        props: FigureTechProperties
    ): OnboardingApiClient = ConfigurationUtil.getDefaultFeignBuilder(mapper)
        .target(OnboardingApiClient::class.java, props.onboardingApiPrefix)
}
