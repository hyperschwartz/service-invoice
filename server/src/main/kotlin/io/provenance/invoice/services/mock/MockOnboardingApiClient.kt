package io.provenance.invoice.services.mock

import io.provenance.invoice.AssetProtos.Asset
import io.provenance.invoice.clients.OnboardingApiClient
import io.provenance.invoice.clients.OnboardingResponse
import mu.KLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["simulated.asset_onboarding_api"], havingValue = "true")
class MockOnboardingApiClient : OnboardingApiClient {
    private companion object : KLogging()

    init {
        logger.warn("App starting with simulated onboarding api client")
    }

    override fun onboardPayable(
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
