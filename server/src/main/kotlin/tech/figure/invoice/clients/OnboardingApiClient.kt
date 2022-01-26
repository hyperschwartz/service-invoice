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
import tech.figure.invoice.util.extension.typedUnpack

@Headers("Content-Type: application/json")
interface OnboardingApiClient {
    @Headers(
        "${AppHeaders.ADDRESS}: {address}",
        "${AppHeaders.PUBLIC_KEY}: {publicKey}",
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
    val writeScopeRequestAny: Any by lazy { json.decodeMessage<MsgWriteScopeRequest>() }
    val writeSessionRequestAny: Any by lazy { json.decodeMessage<MsgWriteSessionRequest>() }
    val writeRecordRequestAny: Any by lazy { json.decodeMessage<MsgWriteRecordRequest>() }
    val writeScopeRequest: MsgWriteScopeRequest by lazy { writeScopeRequestAny.typedUnpack() }
    val writeSessionRequest: MsgWriteSessionRequest by lazy { writeSessionRequestAny.typedUnpack() }
    val writeRecordRequest: MsgWriteRecordRequest by lazy { writeRecordRequestAny.typedUnpack() }

    /**
     * Dynamic unpacking from the source
     */
    private inline fun <reified T: Message> TxBody.decodeMessage(): Any =
        T::class.deriveDefaultInstance().let { Any.pack(it, "") }.typeUrl.let { targetType ->
            this.messagesList.single { it.typeUrl == targetType }
        }
}
