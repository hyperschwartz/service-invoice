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
        logger.info("Generating transactions to board asset [${asset.id.value}]")
        // Tell onboarding api to create a new scope, session, and record that are owned by the requesting wallet, and
        // use the oracle's public key in the call.  Sending the wallet's address ensures that the wallet owns the
        // newly-created scope, and sending the oracle's public key allows the oracle (an account made specifically for
        // this application) can query object store for the asset, allowing us to validate the invoice
        return onboardingApi.generateOnboarding(
            address = walletDetails.address,
            publicKey = provenanceProperties.oraclePublicKey,
            asset = asset,
        ).let { onboardingResponse ->
            AssetOnboardingResponse(
                writeScopeRequest = onboardingResponse.writeScopeRequest,
                writeSessionRequest = onboardingResponse.writeSessionRequest,
                writeRecordRequest = onboardingResponse.writeRecordRequest,
            )
        }
    }
}

data class AssetOnboardingResponse(
    val writeScopeRequest: MsgWriteScopeRequest,
    val writeSessionRequest: MsgWriteSessionRequest,
    val writeRecordRequest: MsgWriteRecordRequest,
)
