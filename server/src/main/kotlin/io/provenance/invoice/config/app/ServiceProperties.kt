package io.provenance.invoice.config.app

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated

@ConstructorBinding
@ConfigurationProperties(prefix = "service")
@Validated
data class ServiceProperties(
    val name: String,
    val environment: String,
    val failStateRetryEnabled: Boolean,
)
