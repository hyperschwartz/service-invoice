package io.provenance.invoice.domain.provenancetx

import com.google.protobuf.ByteString
import io.provenance.invoice.config.app.ConfigurationUtil.DEFAULT_OBJECT_MAPPER
import io.provenance.scope.objectstore.util.base64Encode
import io.provenance.scope.util.toByteString
import java.util.UUID

data class OracleApproval(
    val oracleApproval: OracleApprovalBody
) {
    companion object {
        fun forUuid(payableUuid: UUID): OracleApproval = OracleApproval(
            oracleApproval = OracleApprovalBody(payableUuid)
        )
    }

    fun toBase64Msg(): ByteString = DEFAULT_OBJECT_MAPPER
        .writeValueAsString(this)
        .toByteArray(Charsets.UTF_8)
        .base64Encode()
        .toByteString()
}

data class OracleApprovalBody(val payableUuid: UUID)
