package tech.figure.invoice.testhelpers.testcontainers

import com.fasterxml.jackson.module.kotlin.readValue
import mu.KLogging
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.testcontainers.containers.MockServerContainer
import org.testcontainers.containers.Network
import org.testcontainers.utility.DockerImageName
import tech.figure.invoice.AssetProtos.Asset
import tech.figure.invoice.config.ConfigurationUtil.DEFAULT_OBJECT_MAPPER
import tech.figure.invoice.config.web.AppHeaders
import tech.figure.invoice.testhelpers.AssetOnboardingMocker
import tech.figure.invoice.util.extension.checkNotNull

class MockAssetOnboardingServiceContainer : TestContainerTemplate<MockServerContainer> {
    private companion object : KLogging() {
        // James D Bloom = true MVP
        // The version here must match buildSrc/Dependencies.Versions.MockServer
        private val MOCKSERVER_IMAGE = DockerImageName.parse("jamesdbloom/mockserver:mockserver-5.11.2")
    }

    override val containerName = "Mock Asset Onboarding Service"

    override fun buildContainer(network: Network): MockServerContainer = MockServerContainer(MOCKSERVER_IMAGE)
        .withNetwork(network)

    override fun afterStartup(container: MockServerContainer) {
        MockServerClient(container.containerIpAddress, container.serverPort)
            .`when`(request().withPath("/api/v1/asset"))
            .respond { request ->
                val asset = DEFAULT_OBJECT_MAPPER.readValue<Asset>(request.body.rawBytes)
                val onboardingTx = AssetOnboardingMocker.mockAssetResponse(
                    asset = asset,
                    publicKey = request.headerValue(AppHeaders.WALLET_PUBLIC_KEY),
                    address = request.headerValue(AppHeaders.WALLET_ADDRESS),
                )
                response().withStatusCode(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(DEFAULT_OBJECT_MAPPER.writeValueAsString(onboardingTx))
            }
    }

    override fun getTestProperties(container: MockServerContainer): List<String> = listOf(
        "figure.tech.onboarding_api_prefix=http://${container.containerIpAddress}:${container.serverPort}"
    )

    private fun HttpRequest.headerValue(headerName: String): String = headers
        .entries
        .singleOrNull { it.name.value == headerName }
        .checkNotNull { "No single header with name [$headerName] was present in the request to [${this.path.value}]" }
        .values
        .singleOrNull()
        .checkNotNull { "Request header [$headerName] did not point to a single value" }
        .value
}
