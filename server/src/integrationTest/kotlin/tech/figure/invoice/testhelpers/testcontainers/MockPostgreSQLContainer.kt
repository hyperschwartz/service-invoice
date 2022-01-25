package tech.figure.invoice.testhelpers.testcontainers

import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer

class MockPostgreSQLContainer : TestContainerTemplate<PostgreSQLContainerOverride> {
    override val containerName = "PostgreSQL"

    override fun buildContainer(network: Network): PostgreSQLContainerOverride = PostgreSQLContainerOverride("postgres:13-alpine")
        .withNetwork(network)
        .withNetworkMode(network.id)
        .withDatabaseName("invoice-db")
        .withUsername("postgres")
        .withPassword("password1")

    override fun getTestProperties(container: PostgreSQLContainerOverride): List<String> = listOf(
        "spring.datasource.url=",
        "spring.datasource.username=postgres",
        "spring.datasource.password=password1",
        "database.name=invoice-db",
        "database.username=postgres",
        "database.password=password1",
        "database.hostname=${container.containerIpAddress}",
        "database.port=${container.getMappedPort(5432)}",
        "database.schema=invoice",
        "database.connection_pool_size=10",
    )
}

class PostgreSQLContainerOverride(imageName: String): PostgreSQLContainer<PostgreSQLContainerOverride>(imageName) {
    override fun configure() {
        super.configure()
        setCommand("postgres", "-c", "integrationtest.safe=1", "-c", "fsync=off")
    }
}

