package io.provenance.invoice.components

import io.provenance.invoice.config.eventstream.EventStreamConstants
import io.provenance.invoice.config.eventstream.EventStreamProperties
import io.provenance.invoice.util.eventstream.external.EventBatch
import io.provenance.invoice.util.eventstream.external.EventStreamFactory
import io.provenance.invoice.util.eventstream.external.EventStreamResponseObserver
import io.provenance.invoice.util.eventstream.external.StreamEvent
import io.provenance.invoice.util.extension.wrapList
import mu.KLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.integration.support.locks.LockRegistry
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class EventStreamConsumer(
    private val lockRegistry: LockRegistry,
    private val redisTemplate: RedisTemplate<String, Long>,
    private val eventStreamFactory: EventStreamFactory,
    private val eventStreamProperties: EventStreamProperties,
) {
    private companion object : KLogging() {
        private const val EVENT_STREAM_CONSUMER = "event-stream-consumer-invoice"
        private const val EVENT_STREAM_CONSUMER_HEIGHT = "$EVENT_STREAM_CONSUMER-height"
    }

    // Use a fixed delay to restart the process if the event stream consumer within fails for any reason
    @Scheduled(fixedDelay = EventStreamConstants.EVENT_STREAM_DELAY)
    fun consumeEventStream() {
        val lock = lockRegistry.obtain(EVENT_STREAM_CONSUMER)
        if (lock.tryLock()) {
            try {
                val responseObserver = EventStreamResponseObserver<EventBatch> { batch ->
                    // Handle each observed event
                    batch.events.forEach(::handleEvent)
                    redisTemplate.opsForValue().set(EVENT_STREAM_CONSUMER_HEIGHT, batch.height)
                    logger.info("Processed events and established new height: ${batch.height}")
                }

                val height = redisTemplate.opsForValue().get(EVENT_STREAM_CONSUMER_HEIGHT)
                    ?: eventStreamProperties.epochHeight.toLong()

                logger.info("Starting event stream at height: $height")

                eventStreamFactory.getStream(eventTypes = "wasm".wrapList(), startHeight = height, observer = responseObserver).streamEvents()

                while (true) {
                    val isComplete = responseObserver.finishLatch.await(60, TimeUnit.SECONDS)
                    responseObserver.error?.also { throw it }
                    if (isComplete) {
                        logger.warn("Event stream signalled completed")
                        return
                    }
                    logger.info("Event stream active")
                }
            } finally {
                lock.unlock()
            }
        }
    }

    private fun handleEvent(event: StreamEvent) {
        event.attributes.singleOrNull { it.key == "PAYABLE_REGISTERED" }?.also { attribute ->
            logger.info("Found a live one: ${attribute.value}")
        } ?: run {
            logger.info("Not related")
        }
        //logger.info("We did it! We saw an event! Hash: ${event.txHash}, Type: ${event.eventType}, Attribute Count: ${event.attributes.size}, Height: ${event.height}")
    }
}
