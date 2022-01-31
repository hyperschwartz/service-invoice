package io.provenance.invoice.services

import io.provenance.invoice.AssetProtos.Asset
import io.provenance.invoice.clients.OnboardingApiClient
import io.provenance.metadata.v1.MsgWriteRecordRequest
import io.provenance.metadata.v1.MsgWriteScopeRequest
import io.provenance.metadata.v1.MsgWriteSessionRequest
import mu.KLogging
import org.springframework.stereotype.Service
import io.provenance.invoice.config.provenance.ProvenanceProperties
import io.provenance.invoice.domain.wallet.WalletDetails
import io.provenance.invoice.util.provenance.ProvenanceUtil

@Service
class AssetOnboardingService(
    private val onboardingApi: OnboardingApiClient,
    private val provenanceProperties: ProvenanceProperties,
) {
    private companion object : KLogging()

    fun generateInvoiceBoardingTx(
        asset: Asset,
        walletDetails: WalletDetails,
    ): AssetOnboardingResponse {
        logger.info("Generating marker details from request for asset [${asset.id.value}]")
        val markerDenom = ProvenanceUtil.getInvoiceDenominationForAsset(asset)
        val markerAddress = ProvenanceUtil.generateMarkerAddressForDenomFromSource(
            denom = markerDenom,
            accountAddress = walletDetails.address,
        )
        logger.info("Generating transactions to board asset [${asset.id.value}]")
        // Tell onboarding api to create a new scope, session, and record that are owned by the requesting wallet, and
        // use the oracle's public key in the call.  Sending the wallet's address ensures that the wallet owns the
        // newly-created scope, and sending the oracle's public key allows the oracle (an account made specifically for
        // this application) can query object store for the asset, allowing us to validate the invoice
        val onboardingResponse = onboardingApi.generateOnboarding(
            address = walletDetails.address,
            publicKey = provenanceProperties.oraclePublicKey,
            asset = asset,
        )
        return AssetOnboardingResponse(
            markerDenom = markerDenom,
            markerAddress = markerAddress,
            writeScopeRequest = onboardingResponse.writeScopeRequest.addMarkerAsValueOwner(markerAddress),
            writeSessionRequest = onboardingResponse.writeSessionRequest,
            writeRecordRequest = onboardingResponse.writeRecordRequest,
        )
    }

    /**
     * Each message is created to dictate that the marker address is the signing entity, but in order for the frontend
     * flow to work, the signer must be the wallet.  This change removes the requirement for the marker to sign the
     * contract and pay the fee, and instead allows the wallet to be the entity to make the payment.
     *
     * Each individual type must have its own override because protobuf creates unique objects with no related
     * interfaces.
     */
    private fun MsgWriteScopeRequest.addMarkerAsValueOwner(
        markerAddress: String
    ): MsgWriteScopeRequest = toBuilder().also { writeScopeBuilder ->
        writeScopeBuilder.scopeBuilder.valueOwnerAddress = markerAddress
    }.build()
}

data class AssetOnboardingResponse(
    val markerDenom: String,
    val markerAddress: String,
    val writeScopeRequest: MsgWriteScopeRequest,
    val writeSessionRequest: MsgWriteSessionRequest,
    val writeRecordRequest: MsgWriteRecordRequest,
)
