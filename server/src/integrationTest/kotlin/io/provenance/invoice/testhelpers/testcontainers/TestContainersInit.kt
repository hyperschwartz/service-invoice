package io.provenance.invoice.testhelpers.testcontainers

import mu.KLogging
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import java.util.UUID

/**
 * A bundled ApplicationContextListener implementation that loads up test containers if the spring profile for
 * test-containers is added to the properties on init.  By default, the property is enabled, and this functionality
 * will execute to create a simulated environment.
 *
 */
class TestContainersInit : ApplicationContextInitializer<ConfigurableApplicationContext> {
    private companion object : KLogging() {
        private val DEFAULT_PROPERTIES: Set<String> = setOf(
            // Turning the event stream and retries on in an integration test environment makes no sense.
            // Each individual event stream event should instead be tested alone
            "event.stream.enabled=false",
            "service.fail_state_retry_enabled=false",
        )
    }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        if ("test-containers" !in applicationContext.environment.activeProfiles) {
            logger.error("Test containers are not enabled for this run. Exiting configuration")
            return
        } else {
            logger.info("Test containers spring profile found. Booting simulated environment")
        }

        val testProperties = mutableSetOf<String>()

        listOf(
            MockPostgreSQLContainer(),
            MockRedisContainer(),
            MockAssetOnboardingServiceContainer(),
        ).forEach { containerTemplate ->
            logger.info("Building container with name [${containerTemplate.containerName}]")
            val container = containerTemplate.buildContainer(network)
            logger.info("Starting test container [${containerTemplate.containerName}]")
            container.start()
            logger.info("Running startup hook for container [${containerTemplate.containerName}]")
            containerTemplate.afterStartup(container)
            logger.info("Deriving test properties from container [${containerTemplate.containerName}]")
            testProperties += containerTemplate.getTestProperties(container)
        }

        // Register all non-container-related properties
        testProperties += DEFAULT_PROPERTIES

        // Load in all properties derived by creating all containers
        TestPropertyValues.of(*testProperties.toTypedArray()).applyTo(applicationContext.environment)

        logger.info("Booted test containers with environment: ${applicationContext.environment}")
    }

    private val network: Network by lazy {
        Network.builder().createNetworkCmdModifier {
            it.withName("service-invoice-int-tests-network-${UUID.randomUUID()}")
        }.build()
    }
}

interface TestContainerTemplate<out T: GenericContainer<out T>> {
    val containerName: String

    fun buildContainer(network: Network): T
    fun afterStartup(container: @UnsafeVariance T) {}
    fun getTestProperties(container: @UnsafeVariance T): List<String> = emptyList()
}
