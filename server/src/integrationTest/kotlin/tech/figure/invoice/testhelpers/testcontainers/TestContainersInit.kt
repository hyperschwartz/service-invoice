package tech.figure.invoice.testhelpers.testcontainers

import mu.KLogging
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import java.util.UUID

/**
 * A bundled ApplicationContextListener implementation that loads up test containers if the spring profile for
 * test-containers is added to the properties on init.  By default, the property is enabled, and this functionality
 * will execute to create a simulated environment.
 *
 * TODO: Add a test container for simulating responses from service-asset-onboarding.
 */
class TestContainersInit : ApplicationContextInitializer<ConfigurableApplicationContext> {
    private companion object : KLogging()

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

// FIXME: Find a way to do this without using the UnsafeVariance annotation.  There's gotta be a way!
interface TestContainerTemplate<out T: GenericContainer<@UnsafeVariance T>> {
    val containerName: String

    fun buildContainer(network: Network): T
    fun afterStartup(container: @UnsafeVariance T) {}
    fun getTestProperties(container: @UnsafeVariance T): List<String> = emptyList()
}
