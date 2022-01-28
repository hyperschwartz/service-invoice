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
        // Tell onboarding api to create a new scope, session, and record that are owned by the prospective new marker,
        // and use the wallet as the public key for object store reads.
        val onboardingResponse = onboardingApi.generateOnboarding(
            address = markerAddress,
            publicKey = provenanceProperties.oraclePublicKey,
            asset = asset,
        )
        return AssetOnboardingResponse(
            markerDenom = markerDenom,
            markerAddress = markerAddress,
            writeScopeRequest = onboardingResponse.writeScopeRequest.signWallet(walletDetails.address),
            writeSessionRequest = onboardingResponse.writeSessionRequest.signWallet(walletDetails.address),
            writeRecordRequest = onboardingResponse.writeRecordRequest.signWallet(walletDetails.address),
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
    private fun MsgWriteScopeRequest.signWallet(
        walletAddress: String
    ): MsgWriteScopeRequest = toBuilder().clearSigners().addSigners(walletAddress).build()

    private fun MsgWriteSessionRequest.signWallet(
        walletAddress: String
    ): MsgWriteSessionRequest = toBuilder().clearSigners().addSigners(walletAddress).build()

    private fun MsgWriteRecordRequest.signWallet(
        walletAddress: String
    ): MsgWriteRecordRequest = toBuilder().clearSigners().addSigners(walletAddress).build()
}

data class AssetOnboardingResponse(
    val markerDenom: String,
    val markerAddress: String,
    val writeScopeRequest: MsgWriteScopeRequest,
    val writeSessionRequest: MsgWriteSessionRequest,
    val writeRecordRequest: MsgWriteRecordRequest,
)
