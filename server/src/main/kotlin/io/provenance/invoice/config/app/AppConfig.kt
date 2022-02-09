package io.provenance.invoice.config.app

import com.fasterxml.jackson.databind.ObjectMapper
import io.provenance.invoice.util.coroutine.CoroutineUtil
import kotlinx.coroutines.CoroutineScope
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
@EnableConfigurationProperties(value = [ServiceProperties::class])
class AppConfig {
    @Primary
    @Bean
    fun objectMapper(): ObjectMapper = ConfigurationUtil.DEFAULT_OBJECT_MAPPER

    @Bean(Qualifiers.EVENT_STREAM_COROUTINE_SCOPE)
    fun eventStreamCoroutineScope(): CoroutineScope = CoroutineUtil.newSingletonScope(
        scopeName = CoroutineScopeNames.EVENT_STREAM_SCOPE,
        threadCount = 10,
    )

    @Bean(Qualifiers.FAILURE_RETRY_COROUTINE_SCOPE)
    fun failureRetryCoroutineScope(): CoroutineScope = CoroutineUtil.newSingletonScope(
        scopeName = CoroutineScopeNames.FAILURE_RETRY_SCOPE,
        threadCount = 10,
    )
}
