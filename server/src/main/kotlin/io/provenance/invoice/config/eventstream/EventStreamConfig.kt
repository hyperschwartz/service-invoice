package io.provenance.invoice.config.eventstream

import com.fasterxml.jackson.databind.ObjectMapper
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import io.provenance.invoice.util.eventstream.external.EventStreamFactory
import io.provenance.invoice.util.eventstream.external.RpcClient
import okhttp3.OkHttpClient
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
@EnableConfigurationProperties(value = [EventStreamProperties::class])
class EventStreamConfig {
    @Bean
    fun rpcClient(mapper: ObjectMapper, eventStreamProperties: EventStreamProperties): RpcClient = RpcClient
        .Builder(eventStreamProperties.rpcUri, mapper).build()

    @Bean
    fun eventStreamBuilder(eventStreamProperties: EventStreamProperties): Scarlet.Builder {
        val node = eventStreamProperties.websocketUri
        return Scarlet.Builder()
            .webSocketFactory(
                OkHttpClient.Builder()
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build()
                    .newWebSocketFactory("${node.scheme}://${node.host}:${node.port}/websocket")
            )
            .addMessageAdapterFactory(MoshiMessageAdapter.Factory())
            .addStreamAdapterFactory(RxJava2StreamAdapterFactory())
    }

    @Bean
    fun eventStreamFactory(
        rpcClient: RpcClient,
        eventStreamBuilder: Scarlet.Builder,
    ): EventStreamFactory = EventStreamFactory(rpcClient, eventStreamBuilder)
}
