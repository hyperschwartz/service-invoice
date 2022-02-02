package io.provenance.invoice.util.eventstream.external

import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.LifecycleRegistry

class EventStreamStaleException(message: String) : Throwable(message)

class EventStreamFactory(private val rpcClient: RpcClient, private val eventStreamBuilder: Scarlet.Builder) {

    fun getStream(eventTypes: List<String>, startHeight: Long, observer: EventStreamResponseObserver<EventBatch>): EventStream {
        val lifecycle = LifecycleRegistry(0L)

        return EventStream(
            eventTypes,
            startHeight,
            observer,
            lifecycle,
            eventStreamBuilder.lifecycle(lifecycle).build().create(),
            rpcClient
        )
    }
}
