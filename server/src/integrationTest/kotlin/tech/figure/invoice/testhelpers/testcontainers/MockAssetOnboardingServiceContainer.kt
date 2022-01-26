package tech.figure.invoice.testhelpers.testcontainers

import com.fasterxml.jackson.module.kotlin.readValue
import mu.KLogging
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.testcontainers.containers.MockServerContainer
import org.testcontainers.containers.Network
import org.testcontainers.utility.DockerImageName
import tech.figure.invoice.AssetProtos.Asset
import tech.figure.invoice.config.app.ConfigurationUtil.DEFAULT_OBJECT_MAPPER
import tech.figure.invoice.config.web.AppHeaders
import tech.figure.invoice.services.mock.AssetOnboardingMocker

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
        // Establish mocked routes and their expected responses
        MockServerClient(container.containerIpAddress, container.serverPort)
            // Emulate a call to onboarding api's asset transaction generation. Parallels the call in OnboardingApiClient.kt
            .`when`(request().withPath("/api/v1/asset"))
            .respond { request ->
                val apiKey = request.headerValueOrNull(AppHeaders.API_KEY)
                if (apiKey.isNullOrBlank()) {
                    response().withStatusCode(401)
                        .withJsonContentType()
                        .withBodyAsJson(SimpleMockResponse("Unauthorized"))
                } else {
                    request.tryHandle { req ->
                        val asset = DEFAULT_OBJECT_MAPPER.readValue<Asset>(req.body.rawBytes)
                        val onboardingTx = AssetOnboardingMocker.mockAssetResponse(
                            asset = asset,
                            publicKey = req.headerValue(AppHeaders.PUBLIC_KEY),
                            address = req.headerValue(AppHeaders.ADDRESS),
                        )
                        response().withStatusCode(200)
                            .withJsonContentType()
                            .withBodyAsJson(onboardingTx)
                    }
                }
            }
    }

    override fun getTestProperties(container: MockServerContainer): List<String> = listOf(
        // Ensure that calls to the onboarding api are funneled through this container
        "figure.tech.onboarding_api_prefix=http://${container.containerIpAddress}:${container.serverPort}"
    )
}
