package io.provenance.name.wallet.components

import io.provenance.client.PbClient
import io.provenance.name.wallet.services.NameService
import mu.KLogging
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean

@Component
class ProvenanceListener(
    private val nameService: NameService,
    private val pbClient: PbClient,
) {
    private companion object : KLogging() {
        private val listenerActivated: AtomicBoolean = AtomicBoolean(false)
    }

    @EventListener(ApplicationReadyEvent::class)
    fun initializeListener() {
        if (listenerActivated.get()) {
            logger.error("Attempted duplicate init for provenance listening!")
            return
        }
        logger.info("Initializing provenance listener for address [${pbClient.channelUri.path}]")
        listenerActivated.set(true)
    }
}
