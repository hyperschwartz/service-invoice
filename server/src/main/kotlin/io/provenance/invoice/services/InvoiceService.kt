package io.provenance.invoice.services

import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.InvoiceProtos.LineItem
import io.provenance.invoice.clients.OnboardingApiClient
import io.provenance.invoice.repository.InvoiceRepository
import io.provenance.invoice.util.enums.ExpectedDenom
import io.provenance.invoice.util.enums.InvoiceProcessingStatus
import io.provenance.invoice.util.extension.check
import io.provenance.invoice.util.extension.checkNotNull
import io.provenance.invoice.util.extension.isAfterInclusive
import io.provenance.invoice.util.extension.isBeforeInclusive
import io.provenance.invoice.util.extension.toAsset
import io.provenance.invoice.util.extension.toBigDecimalOrNull
import io.provenance.invoice.util.extension.toLocalDateOrNull
import io.provenance.invoice.util.extension.toUuidOrNull
import io.provenance.invoice.util.extension.totalAmount
import mu.KLogging
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Service
class InvoiceService(
    private val invoiceRepository: InvoiceRepository,
    private val onboardingApi: OnboardingApiClient,
) {
    private companion object : KLogging()

    // TODO: Return onboarding API results for the invoice instead of just validate / insert / return
    fun onboardInvoice(
        invoice: Invoice,
        address: String,
        publicKey: String,
    ): Invoice {
        // Ensure first that the invoice is of a valid format
        logger.info("Validating received invoice with uuid [${invoice.invoiceUuid.value}]")
        validateInvoice(invoice)
        logger.info("Generating onboarding asset from invoice with uuid [${invoice.invoiceUuid.value}]")
        val asset = invoice.toAsset()
        logger.info("Generating onboarding messages for invoice with uuid [${invoice.invoiceUuid.value}]")
        // TODO: This needs an api key
        // TODO: Need to store the results from this in the db alongside the invoice to enable retries
        try {
            onboardingApi.generateOnboarding(
                address = address,
                publicKey = publicKey,
                asset = asset,
            ).also { results -> logger.info("Successful onboarding api call: $results") }
        } catch (e: Exception) {
            logger.error("Failed to generate onboarding payload with onboarding api", e)
        }
        logger.info("Storing successful payload in the database for invoice [${invoice.invoiceUuid.value}]")
        // Acknowledge receipt of invoice by upserting it to the database
        return invoiceRepository.upsert(invoice = invoice, status = InvoiceProcessingStatus.PENDING_STAMP)
    }

    private fun validateInvoice(invoice: Invoice) {
        // Verify UUID is formatted correctly
        val invoiceUuid = invoice.invoiceUuid.toUuidOrNull()
        checkNotNull(invoiceUuid) { "Invoice provided has an invalid invoiceUuid value [${invoice.invoiceUuid.value}]" }

        val errorPrefix = "Validation for invoice [$invoiceUuid] failed:"

        // Verify an invoice with the same uuid does not exist
        check(invoiceRepository.findByUuidOrNull(invoiceUuid) == null) { "$errorPrefix Invoice uuid matches previously uploaded invoice" }

        // Verify that addresses are properly formatted
        // TODO: Use some magic util to make sure these are actual provenance addresses unless test mode is on or something
        check(invoice.fromAddress.isNotBlank()) { "$errorPrefix The sender address must not be blank" }
        check(invoice.toAddress.isNotBlank()) { "$errorPrefix The receiver address must not be blank" }

        // Verify that created / due dates are properly formatted
        val today = LocalDate.now()
        val createdDate = invoice.invoiceCreatedDate.toLocalDateOrNull().checkNotNull { "$errorPrefix Created date was not provided" }
        check(createdDate.isBeforeInclusive(LocalDate.now())) { "$errorPrefix Created date [$createdDate] cannot be in the future [today: $today]" }
        invoice.invoiceDueDate.toLocalDateOrNull().also { dueDate ->
            checkNotNull(dueDate) { "$errorPrefix Due date was not provided" }
            check(dueDate.isAfterInclusive(createdDate)) { "$errorPrefix Due date [$dueDate] cannot come before the created date [$createdDate]" }
        }

        // Verify that string qualifiers are defined
        check(invoice.description.isNotBlank()) { "$errorPrefix The description must not be blank" }
        check(invoice.paymentDenom in ExpectedDenom.ALL_EXPECTED_NAMES) { "$errorPrefix Unexpected denom: [${invoice.paymentDenom}]" }

        // Verify line items
        check(invoice.lineItemsList.isNotEmpty()) { "$errorPrefix Invoice contained no line items" }
        val invoiceTotal = invoice.totalAmount()
        check(invoiceTotal > BigDecimal.ZERO) { "$errorPrefix Invoice charge sum [$invoiceTotal] must be greater than zero" }
        invoice.lineItemsList.forEach { validateLineItem(invoiceUuid, it) }
    }

    private fun validateLineItem(invoiceUuid: UUID, lineItem: LineItem) {
        // Verify UUID is formatted correctly
        val lineUuid = lineItem.lineUuid.toUuidOrNull()
        checkNotNull(lineUuid) { "Invoice [$invoiceUuid] Line Item with name [${lineItem.name}] and description [${lineItem.description}] had no uuid" }

        val errorPrefix = "Validation for invoice [$invoiceUuid] line [$lineUuid] w/ name [${lineItem.name}] failed:"

        // Verify that name is set
        check(lineItem.name.isNotBlank()) { "$errorPrefix The name must be provided" }
        check(lineItem.description.isNotBlank()) { "$errorPrefix The description must be provided" }
        check(lineItem.quantity > 0) { "$errorPrefix Each line item must have a quantity of at least one. Provided: [${lineItem.quantity}]" }
        lineItem.price.toBigDecimalOrNull()
            .checkNotNull { "$errorPrefix Each line item must have a defined price, but was [${lineItem.price.value}]" }
            .check({ it > BigDecimal.ZERO }) { "$errorPrefix Each line item must have a price greater than zero, but was [${lineItem.price.value}]" }
    }
}
