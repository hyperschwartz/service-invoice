package io.provenance.invoice.clients

import cosmos.tx.v1beta1.TxOuterClass
import feign.Headers
import feign.Param
import feign.RequestLine
import io.provenance.invoice.AssetProtos.Asset
import io.provenance.invoice.config.web.AppHeaders

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
    val json: TxOuterClass.TxBody,
    // Each individual message in the transaction is returned as a Base64 encoded string
    val base64: List<String>,
)
