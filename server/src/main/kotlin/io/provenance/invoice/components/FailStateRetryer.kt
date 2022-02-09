package io.provenance.invoice.components

import io.provenance.invoice.config.app.ServiceProperties
import io.provenance.invoice.repository.FailedEventRepository
import io.provenance.invoice.services.EventHandlerService
import io.provenance.invoice.util.eventstream.external.RpcClient
import io.provenance.invoice.util.extension.toStreamEventI
import io.provenance.invoice.util.provenance.PayableContractKey
import mu.KLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * This component detects invoices that have failed onboarding and attempts to fix the issue by re-running their
 * onboarding process.
 */
@Component
class FailStateRetryer(
    private val eventHandlerService: EventHandlerService,
    private val failedEventRepository: FailedEventRepository,
    private val rpcClient: RpcClient,
    private val serviceProperties: ServiceProperties,
) {
    private companion object : KLogging() {
        private const val RETRY_MINUTES: Long = 5
        private const val CONTRACT_EVENT_TYPE = "wasm"
    }

    @Scheduled(fixedRate = RETRY_MINUTES, timeUnit = TimeUnit.MINUTES)
    fun retryFailedEventProcessing() {
        if (!serviceProperties.failStateRetryEnabled) {
            logger.error("FAILURE RETRY DISABLED. THIS SHOULD NOT OCCUR IN A LIVE DEPLOYMENT")
            return
        }
        val logPrefix = "[RETRY FAILED EVENTS]:"
        logger.info("$logPrefix Searching for failed events")
        val failedEventTxHashes = failedEventRepository.findAllFailedEvents()
        if (failedEventTxHashes.isEmpty()) {
            logger.info("$logPrefix No failed events detected")
            return
        }
        logger.info("$logPrefix Found [${failedEventTxHashes.size}] failed event(s) to retry")
        failedEventTxHashes.forEach { txHash ->
            logger.info("$logPrefix Fetching transaction details for event hash [$txHash]")
            try {
                rpcClient.getTransaction(txHash)
            } catch (e: Exception) {
                logger.error("$logPrefix Failed to fetch transaction for hash [$txHash]", e)
                null
            }?.also { txResponse ->
                logger.info("$logPrefix Retrying event with hash [$txHash]")
                txResponse
                    .result
                    ?.txResult
                    ?.events
                    ?.filter { it.type == CONTRACT_EVENT_TYPE }
                    ?.filter { it.attributes.any { attribute -> attribute.key in PayableContractKey.EVENT_KEYS_CONTRACT_NAMES } }
                    ?.forEach { actionableEvent -> eventHandlerService.handleEvent(event = actionableEvent.toStreamEventI(txResponse.result), isRetry = true) }
            }
        }
    }
}
