package tech.figure.invoice.config.figuretech

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated

@ConstructorBinding
@ConfigurationProperties(prefix = "figure.tech")
@Validated
data class FigureTechProperties(
    val onboardingApiPrefix: String,
    // When deployed internally, the apikey is not necessary
    val onboardingApiKey: String?,
)
