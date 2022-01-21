package io.provenance.invoice.clients

import com.fasterxml.jackson.databind.node.ObjectNode
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
    ): TxBody
}

data class TxBody(
    val json: ObjectNode,
    val base64: List<String>,
)
