package io.provenance.invoice.config.redis

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.integration.redis.util.RedisLockRegistry
import org.springframework.integration.support.locks.LockRegistry

@Configuration
class RedisConfig {
    @Bean
    fun redisLockRegistry(redisConnectionFactory: RedisConnectionFactory): LockRegistry =
        RedisLockRegistry(redisConnectionFactory, "lock")

    @Bean
    fun redisTemplate(redisConnectionFactory: RedisConnectionFactory): RedisTemplate<String, Long> = RedisTemplate<String, Long>().apply {
        setConnectionFactory(redisConnectionFactory)
    }
}
