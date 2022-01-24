package tech.figure.invoice.services

import tech.figure.invoice.InvoiceProtos.Invoice
import mu.KLogging
import org.springframework.stereotype.Service
import tech.figure.invoice.domain.wallet.WalletDetails
import tech.figure.invoice.repository.InvoiceRepository
import tech.figure.invoice.util.enums.InvoiceProcessingStatus
import tech.figure.invoice.util.extension.toAsset
import tech.figure.invoice.util.extension.toUuid
import tech.figure.invoice.util.validation.InvoiceValidator

@Service
class InvoiceService(
    private val assetOnboardingService: AssetOnboardingService,
    private val invoiceRepository: InvoiceRepository,
) {
    private companion object : KLogging()

    // TODO: Return onboarding API results for the invoice instead of just validate / insert / return
    fun onboardInvoice(request: OnboardInvoiceRequest): Invoice {
        // Ensure first that the invoice is of a valid format
        logger.info("Validating received invoice with uuid [${request.invoice.invoiceUuid.value}] has not yet been boarded")
        InvoiceValidator.validateInvoice(request.invoice)
        logger.info("Verifying invoice with uuid [${request.invoice.invoiceUuid.value}] has not yet been boarded")
        // Verify an invoice with the same uuid does not exist
        check(invoiceRepository.findByUuidOrNull(request.invoice.invoiceUuid.toUuid()) == null) { "Requested invoice uuid [${request.invoice.invoiceUuid.value}] matches previously uploaded invoice" }
        logger.info("Generating onboarding asset from invoice with uuid [${request.invoice.invoiceUuid.value}]")
        val asset = request.invoice.toAsset()
        logger.info("Generating onboarding messages for invoice with uuid [${request.invoice.invoiceUuid.value}]")
        // TODO: Need to store the results from this in the db alongside the invoice to enable retries
        try {
            assetOnboardingService.generateOnboardingTransactions(asset = asset, walletDetails = request.walletDetails)
                .also { results -> logger.info("Successful onboarding api call: $results") }
        } catch (e: Exception) {
            logger.error("Failed to generate onboarding payload with onboarding api", e)
        }
        logger.info("Storing successful payload in the database for invoice [${request.invoice.invoiceUuid.value}]")
        // Acknowledge receipt of invoice by upserting it to the database
        return invoiceRepository.upsert(invoice = request.invoice, status = InvoiceProcessingStatus.PENDING_STAMP)
    }
}

data class OnboardInvoiceRequest(
    val invoice: Invoice,
    val walletDetails: WalletDetails,
)
