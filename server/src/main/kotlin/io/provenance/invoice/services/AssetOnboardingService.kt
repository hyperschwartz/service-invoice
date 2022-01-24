package io.provenance.invoice.services

import io.provenance.invoice.AssetProtos.Asset
import io.provenance.invoice.clients.OnboardingApiClient
import io.provenance.invoice.clients.OnboardingResponse
import io.provenance.invoice.domain.wallet.WalletDetails
import mu.KLogging
import org.springframework.stereotype.Service

@Service
class AssetOnboardingService(private val onboardingApi: OnboardingApiClient) {
    private companion object : KLogging()

    fun generateOnboardingTransactions(
        asset: Asset,
        walletDetails: WalletDetails,
    ): OnboardingResponse {
        logger.info("Generating transactions to board asset [${asset.id.value}]")
        // TODO: Verify that the payload looks correct in here or throw an exception
        return onboardingApi.generateOnboarding(
            address = walletDetails.address,
            publicKey = walletDetails.publicKey,
            asset = asset,
        )
    }
}
