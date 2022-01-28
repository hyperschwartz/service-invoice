package tech.figure.invoice.services

import com.google.protobuf.Any
import io.provenance.metadata.v1.MsgWriteRecordRequest
import io.provenance.metadata.v1.MsgWriteScopeRequest
import io.provenance.metadata.v1.MsgWriteSessionRequest
import tech.figure.invoice.AssetProtos.Asset
import mu.KLogging
import org.springframework.stereotype.Service
import tech.figure.invoice.clients.OnboardingApiClient
import tech.figure.invoice.domain.wallet.WalletDetails
import tech.figure.invoice.util.extension.toProtoAny
import tech.figure.invoice.util.extension.typedUnpack
import tech.figure.invoice.util.provenance.ProvenanceAddressUtil

@Service
class AssetOnboardingService(private val onboardingApi: OnboardingApiClient) {
    private companion object : KLogging() {
        private const val DENOM_PREFIX = "invoice"

        fun getDenominationForAsset(asset: Asset): String = "$DENOM_PREFIX-${asset.id.value}"
    }

    fun generateOnboardingTransactions(
        asset: Asset,
        walletDetails: WalletDetails,
    ): AssetOnboardingResponse {
        logger.info("Generating marker details from request for asset [${asset.id.value}]")
        val markerDenom = getDenominationForAsset(asset)
        val markerAddress = ProvenanceAddressUtil.generateMarkerAddressForDenomFromSource(
            denom = markerDenom,
            accountAddress = walletDetails.address,
        )
        logger.info("Generating transactions to board asset [${asset.id.value}]")
        // Tell onboarding api to create a new scope, session, and record that are owned by the prospective new marker,
        // and use the wallet as the public key for object store reads.
        // TODO: Create an oracle account that is tied to the application, that will be able to read object store
        // TODO: and verify that the correct invoice was added
        val onboardingResponse = onboardingApi.generateOnboarding(
            address = markerAddress,
            // TODO: Create a public key for the oracle account and just store it in the app
            publicKey = walletDetails.publicKey,
            asset = asset,
        )
        return AssetOnboardingResponse(
            markerDenom = markerDenom,
            markerAddress = markerAddress,
            writeScopeRequest = onboardingResponse.writeScopeRequest.typedUnpack<MsgWriteScopeRequest>().rePackWithAddressSigner(walletDetails.address),
            writeSessionRequest = onboardingResponse.writeSessionRequest.typedUnpack<MsgWriteSessionRequest>().rePackWithAddressSigner(walletDetails.address),
            writeRecordRequest = onboardingResponse.writeRecordRequest.typedUnpack<MsgWriteRecordRequest>().rePackWithAddressSigner(walletDetails.address),
        )
    }

    private fun MsgWriteScopeRequest.rePackWithAddressSigner(walletAddress: String): Any = toBuilder().also { builder ->
        builder.clearSigners()
        builder.addSigners(walletAddress)
    }.build().toProtoAny()

    private fun MsgWriteSessionRequest.rePackWithAddressSigner(walletAddress: String): Any = toBuilder().also { builder ->
        builder.clearSigners()
        builder.addSigners(walletAddress)
    }.build().toProtoAny()

    private fun MsgWriteRecordRequest.rePackWithAddressSigner(walletAddress: String): Any = toBuilder().also { builder ->
        builder.clearSigners()
        builder.addSigners(walletAddress)
    }.build().toProtoAny()
}

data class AssetOnboardingResponse(
    val markerDenom: String,
    val markerAddress: String,
    val writeScopeRequest: Any,
    val writeSessionRequest: Any,
    val writeRecordRequest: Any,
)
