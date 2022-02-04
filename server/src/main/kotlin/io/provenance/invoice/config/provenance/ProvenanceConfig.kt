package io.provenance.invoice.config.provenance

import io.provenance.client.PbClient
import io.provenance.invoice.config.app.Qualifiers
import io.provenance.invoice.config.app.ServiceProperties
import io.provenance.invoice.util.provenance.ProvenanceAccountDetail
import io.provenance.scope.objectstore.client.OsClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(value = [ProvenanceProperties::class])
class ProvenanceConfig {
    @Bean
    fun pbClient(provenanceProperties: ProvenanceProperties): PbClient = PbClient(
        chainId = provenanceProperties.chainId,
        channelUri = provenanceProperties.channelUri,
    )

    @Bean(Qualifiers.ORACLE_ACCOUNT_DETAIL)
    fun oracleAccountDetail(
        provenanceProperties: ProvenanceProperties,
        serviceProperties: ServiceProperties
    ): ProvenanceAccountDetail = ProvenanceAccountDetail.fromBase64PrivateKey(
        privateKeyEncoded = provenanceProperties.oraclePrivateKeyEncoded,
        mainNet = serviceProperties.environment == "prod",
    )

    @Bean
    fun objectStore(
        provenanceProperties: ProvenanceProperties,
        @Qualifier(Qualifiers.ORACLE_ACCOUNT_DETAIL) oracleAccountDetail: ProvenanceAccountDetail,
    ): ObjectStore = ObjectStore(
        osClient = OsClient(
            uri = provenanceProperties.objectStoreUri,
            deadlineMs = provenanceProperties.objectStoreTimeoutMs,
        ),
        oracleAccountDetail = oracleAccountDetail,
    )
}

data class ObjectStore(val osClient: OsClient, val oracleAccountDetail: ProvenanceAccountDetail)
