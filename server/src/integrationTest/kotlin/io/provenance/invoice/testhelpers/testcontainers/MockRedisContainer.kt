package io.provenance.invoice.testhelpers.testcontainers

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network

class MockRedisContainer : TestContainerTemplate<RedisContainerOverride> {
    override val containerName = "Redis"

    override fun buildContainer(network: Network): RedisContainerOverride = RedisContainerOverride("redis:5.0.7")
        .withNetwork(network)
        .withNetworkMode(network.id)
        .withExposedPorts(6379)
}

class RedisContainerOverride(imageName: String): GenericContainer<RedisContainerOverride>(imageName)
