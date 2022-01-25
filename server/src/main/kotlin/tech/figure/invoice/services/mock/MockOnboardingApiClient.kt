package tech.figure.invoice.services.mock

import mu.KLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import tech.figure.invoice.AssetProtos.Asset
import tech.figure.invoice.clients.OnboardingApiClient
import tech.figure.invoice.clients.OnboardingResponse

@Service
@ConditionalOnProperty(name = ["simulated.asset_onboarding_api"], havingValue = "true")
class MockOnboardingApiClient : OnboardingApiClient {
    private companion object : KLogging()

    init {
        logger.warn("App starting with simulated onboarding api client")
    }

    override fun generateOnboarding(
        address: String,
        publicKey: String,
        asset: Asset
    ): OnboardingResponse {
        logger.error("Using simulated asset onboarding api client!")
        return AssetOnboardingMocker.mockAssetResponse(
            asset = asset,
            publicKey = publicKey,
            address = address,
        )
    }
}
