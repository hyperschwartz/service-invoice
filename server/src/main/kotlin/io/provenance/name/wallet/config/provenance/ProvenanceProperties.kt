package io.provenance.name.wallet.config.provenance

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import java.net.URI

@ConstructorBinding
@ConfigurationProperties(prefix = "provenance")
@Validated
data class ProvenanceProperties(
    val chainId: String,
    val channelUri: URI,
)
