package io.provenance.invoice.domain.provenancetx

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.google.protobuf.ByteString
import io.provenance.invoice.config.app.ConfigurationUtil.DEFAULT_OBJECT_MAPPER
import io.provenance.scope.util.toByteString
import java.util.UUID

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class OracleApproval(
    val oracleApproval: OracleApprovalBody
) {
    companion object {
        fun forUuid(payableUuid: UUID): OracleApproval = OracleApproval(
            oracleApproval = OracleApprovalBody(payableUuid)
        )
    }

    fun toBase64Msg(): ByteString = DEFAULT_OBJECT_MAPPER.writeValueAsString(this).toByteString()
}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class OracleApprovalBody(val payableUuid: UUID)
