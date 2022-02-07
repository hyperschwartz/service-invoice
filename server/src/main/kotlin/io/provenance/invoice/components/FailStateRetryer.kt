package io.provenance.invoice.components

import io.provenance.invoice.repository.InvoiceRepository
import io.provenance.invoice.services.ProvenanceQueryService
import mu.KLogging
import org.springframework.integration.support.locks.LockRegistry
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * This component detects invoices that have failed onboarding and attempts to fix the issue by re-running their
 * onboarding process.
 */
@Component
class FailStateRetryer(
    private val invoiceRepository: InvoiceRepository,
    private val lockRegistry: LockRegistry,
    private val provenanceQueryService: ProvenanceQueryService,
) {
    private companion object : KLogging() {
        private const val FAIL_STATE_RETRY_LOCK = "fail-state-retry-invoice"
        // Retry every five minutes
        private const val RETRY_INTERVAL: Long = 5 * 1000 * 60
    }

    @Scheduled(fixedRate = RETRY_INTERVAL)
    fun retryFailedOracleApprovals() {
        val lock = lockRegistry.obtain(FAIL_STATE_RETRY_LOCK)
        val logPrefix = "RETRY ORACLE APPROVALS"
        if (!lock.tryLock()) {
            logger.info("[$logPrefix]: Another retry process is running. Trying again in [${RETRY_INTERVAL}ms]")
            return
        }
        try {
            logger.info("[$logPrefix]: Searching for failed approvals")
            val failedUuids = invoiceRepository.findInvoiceUuidsWithFailedOracleApprovals()
            if (failedUuids.isEmpty()) {
                logger.info("[$logPrefix]: No failed status invoices detected")
                return
            }
            logger.info("[$logPrefix]: Found [${failedUuids.size}] failed invoice(s) to retry")
            failedUuids.forEach { invoiceUuid ->
                provenanceQueryService.submitOracleApproval(
                    invoiceUuid = invoiceUuid,
                    logPrefix = "[$logPrefix, Invoice = $invoiceUuid]:",
                )
            }
        } finally {
            lock.unlock()
        }
    }
}
