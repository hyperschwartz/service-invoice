package tech.figure.invoice.services

import tech.figure.invoice.AssetProtos.Asset
import mu.KLogging
import org.springframework.stereotype.Service
import tech.figure.invoice.clients.OnboardingApiClient
import tech.figure.invoice.clients.OnboardingResponse
import tech.figure.invoice.domain.wallet.WalletDetails

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