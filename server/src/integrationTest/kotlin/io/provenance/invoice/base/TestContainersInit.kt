package io.provenance.invoice.base

import mu.KLogging
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
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

        postgresContainer.start()

        TestPropertyValues.of(
            "spring.datasource.url=",
            "spring.datasource.username=postgres",
            "spring.datasource.password=password1",
            "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
            "database.name=provenance-invoice",
            "database.username=postgres",
            "database.password=password1",
            "database.hostname=${postgresContainer.containerIpAddress}",
            "database.port=${postgresContainer.getMappedPort(5432)}",
            "database.schema=invoice",
            "database.connection_pool_size=10",
        ).applyTo(applicationContext.environment)

        logger.info("Booted test containers with enviornment: ${applicationContext.environment}")
    }

    private val network: Network by lazy {
        Network.builder().createNetworkCmdModifier {
            it.withName("service-invoice-int-tests-network-${UUID.randomUUID()}")
        }.build()
    }

    private val postgresContainer by lazy {
        KPostgreSQLContainer("postgres:13-alpine")
            .withNetwork(network)
            .withNetworkMode(network.id)
            .withDatabaseName("provenance-invoice")
            .withUsername("postgres")
            .withPassword("password1")
    }
}

class KPostgreSQLContainer(imageName: String): PostgreSQLContainer<KPostgreSQLContainer>(imageName) {
    override fun configure() {
        super.configure()
        setCommand("postgres", "-c", "integrationtest.safe=1", "-c", "fsync=off")
    }
}
