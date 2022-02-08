package io.provenance.invoice.components

import io.provenance.invoice.repository.InvoiceRepository
import io.provenance.invoice.services.ProvenanceQueryService
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
    private val invoiceRepository: InvoiceRepository,
    private val provenanceQueryService: ProvenanceQueryService,
) {
    private companion object : KLogging() {
        private const val RETRY_MINUTES: Long = 5
    }

    // Retry every five minutes
    @Scheduled(fixedRate = RETRY_MINUTES, timeUnit = TimeUnit.MINUTES)
    fun retryFailedOracleApprovals() {
        val logPrefix = "RETRY ORACLE APPROVALS"
        logger.info("[$logPrefix]: Searching for failed approvals")
        val failedUuids = invoiceRepository.findInvoiceUuidsWithFailedOracleApprovals()
        if (failedUuids.isEmpty()) {
            logger.info("[$logPrefix]: No failed status invoices detected")
            return
        }
        logger.info("[$logPrefix]: Found [${failedUuids.size}] failed invoice(s) to retry")
        failedUuids.forEach { invoiceUuid ->
            try {
                provenanceQueryService.submitOracleApproval(
                    invoiceUuid = invoiceUuid,
                    logPrefix = "[$logPrefix, Invoice = $invoiceUuid]:",
                )
            } catch (e: Exception) {
                logger.error("[$logPrefix]: Failed to retry oracle approval for invoice [$invoiceUuid]", e)
            }
        }
    }
}
