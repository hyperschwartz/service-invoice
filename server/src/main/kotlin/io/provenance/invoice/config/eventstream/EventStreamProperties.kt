package io.provenance.invoice.config.eventstream

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.net.URI

@ConfigurationProperties(prefix = "event.stream")
@ConstructorBinding
data class EventStreamProperties(
    val websocketUri: URI,
    val rpcUri: String,
    val epochHeight: String,
)
