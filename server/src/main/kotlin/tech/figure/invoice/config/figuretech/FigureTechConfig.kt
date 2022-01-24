package tech.figure.invoice.config.figuretech

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tech.figure.invoice.clients.OnboardingApiClient
import tech.figure.invoice.config.ConfigurationUtil

@Configuration
@EnableConfigurationProperties(value = [FigureTechProperties::class])
class FigureTechConfig {
    @Bean
    fun onboardingApiClient(
        mapper: ObjectMapper,
        props: FigureTechProperties
    ): OnboardingApiClient = ConfigurationUtil.getDefaultFeignBuilder(mapper = mapper, apiKey = props.onboardingApiKey)
        .target(OnboardingApiClient::class.java, props.onboardingApiPrefix)
}
