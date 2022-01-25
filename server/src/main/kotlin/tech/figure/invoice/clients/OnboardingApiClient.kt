package tech.figure.invoice.clients

import com.google.protobuf.Any
import com.google.protobuf.Message
import cosmos.tx.v1beta1.TxOuterClass.TxBody
import feign.Headers
import feign.Param
import feign.RequestLine
import io.provenance.metadata.v1.MsgWriteRecordRequest
import io.provenance.metadata.v1.MsgWriteScopeRequest
import io.provenance.metadata.v1.MsgWriteSessionRequest
import tech.figure.invoice.AssetProtos.Asset
import tech.figure.invoice.config.web.AppHeaders
import tech.figure.invoice.util.extension.deriveDefaultInstance

@Headers("Content-Type: application/json")
interface OnboardingApiClient {
    @Headers(
        "${AppHeaders.WALLET_ADDRESS}: {address}",
        "${AppHeaders.WALLET_PUBLIC_KEY}: {publicKey}",
    )
    @RequestLine("POST /api/v1/asset?permissionAssetManager=false")
    fun generateOnboarding(
        @Param("address") address: String,
        @Param("publicKey") publicKey: String,
        asset: Asset,
    ): OnboardingResponse
}

data class OnboardingResponse(
    // service-asset-onboarding returns an encoded Cosmos TxBody proto labeled as "json," serialized as an ObjectNode.
    // The underlying value is a TxBody, so we can just deserialize straight to the source here
    val json: TxBody,
    // Each individual message in the transaction is returned as a Base64 encoded string
    val base64: List<String>,
) {
    val writeScopeRequest: MsgWriteScopeRequest by lazy { json.decodeMessage() }
    val writeSessionRequest: MsgWriteSessionRequest by lazy { json.decodeMessage() }
    val writeRecordRequest: MsgWriteRecordRequest by lazy { json.decodeMessage() }

    /**
     * Dynamic unpacking from the source
     */
    private inline fun <reified T: Message> TxBody.decodeMessage(): T =
        T::class.deriveDefaultInstance().let(Any::pack).typeUrl.let { targetType ->
            json.messagesList.single { it.typeUrl == targetType }
                .unpack(T::class.java)
        }
}
