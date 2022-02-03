package io.provenance.invoice.config.provenance

import io.provenance.client.PbClient
import io.provenance.scope.encryption.model.DirectKeyRef
import io.provenance.scope.encryption.model.KeyRef
import io.provenance.scope.objectstore.client.OsClient
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.security.KeyPair

@Configuration
@EnableConfigurationProperties(value = [ProvenanceProperties::class])
class ProvenanceConfig {
    @Bean
    fun pbClient(provenanceProperties: ProvenanceProperties): PbClient = PbClient(
        chainId = provenanceProperties.chainId,
        channelUri = provenanceProperties.channelUri,
    )

    @Bean
    fun objectStore(provenanceProperties: ProvenanceProperties): ObjectStore = ObjectStore(
        osClient = OsClient(
            uri = provenanceProperties.objectStoreUri,
            deadlineMs = provenanceProperties.objectStoreTimeoutMs,
        ),
        oracleCredentials = provenanceProperties.oracleKeyPair,
    )
}

data class ObjectStore(
    val osClient: OsClient,
    val oracleCredentials: KeyPair,
) {
    val keyRef: KeyRef by lazy { DirectKeyRef(oracleCredentials) }
}
