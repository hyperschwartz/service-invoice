package io.provenance.invoice.clients

import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.protobuf.Any
import com.google.protobuf.Message
import cosmos.tx.v1beta1.TxOuterClass.TxBody
import feign.Headers
import feign.Param
import feign.RequestLine
import io.provenance.invoice.AssetProtos.Asset
import io.provenance.invoice.config.web.AppHeaders
import io.provenance.invoice.util.extension.checkNotNullI
import io.provenance.invoice.util.extension.deriveDefaultInstanceI
import io.provenance.invoice.util.extension.mergeFromJsonProvenanceI
import io.provenance.invoice.util.extension.typedUnpackI
import io.provenance.metadata.v1.MsgWriteRecordRequest
import io.provenance.metadata.v1.MsgWriteScopeRequest
import io.provenance.metadata.v1.MsgWriteSessionRequest

@Headers("Content-Type: application/json")
interface OnboardingApiClient {
    @Headers(
        "${AppHeaders.ADDRESS}: {address}",
        "${AppHeaders.PUBLIC_KEY}: {publicKey}",
    )
    @RequestLine("POST /api/v1/asset?permissionAssetManager=true&type=payable")
    fun onboardPayable(
        @Param("address") address: String,
        @Param("publicKey") publicKey: String,
        asset: Asset,
    ): OnboardingResponse
}

data class OnboardingResponse(
    // service-asset-onboarding returns an encoded Cosmos TxBody proto labeled as "json," serialized as an ObjectNode.
    // The underlying value is a TxBody, so we can just deserialize straight to the source here
    val json: ObjectNode,
    // Each individual message in the transaction is returned as a Base64 encoded string
    val base64: List<String>,
) {
    private val txBody: TxBody by lazy { TxBody.newBuilder().mergeFromJsonProvenanceI(json).build() }

    val writeScopeRequest: MsgWriteScopeRequest by lazy { txBody.decodeMessage() }
    val writeSessionRequest: MsgWriteSessionRequest by lazy { txBody.decodeMessage() }
    val writeRecordRequest: MsgWriteRecordRequest by lazy { txBody.decodeMessage() }

    override fun toString() = "Received:${System.lineSeparator()}tx body: $json${System.lineSeparator()}base64: $base64"

    /**
     * Dynamic unpacking from the source
     */
    private inline fun <reified T: Message> TxBody.decodeMessage(): T =
        T::class.deriveDefaultInstanceI().let { Any.pack(it, "") }.typeUrl.let { targetType ->
            this.messagesList.singleOrNull { it.typeUrl == targetType }
                .checkNotNullI { "Expected response payload to contain an message of type [${T::class.qualifiedName}] but only contained types: ${txBody.messagesList.map { it.typeUrl }}" }
                .typedUnpackI()
        }
}
