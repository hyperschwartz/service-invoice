package io.provenance.invoice.util.validation

import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.InvoiceProtos.LineItem
import io.provenance.invoice.domain.dto.InvoiceDto
import io.provenance.invoice.util.enums.ExpectedDenom
import io.provenance.invoice.util.extension.checkI
import io.provenance.invoice.util.extension.checkNotNullI
import io.provenance.invoice.util.extension.isAfterInclusiveI
import io.provenance.invoice.util.extension.isBeforeInclusiveI
import io.provenance.invoice.util.extension.scopeIdI
import io.provenance.invoice.util.extension.toBigDecimalOrNullI
import io.provenance.invoice.util.extension.toLocalDateOrNullI
import io.provenance.invoice.util.extension.toUuidOrNullI
import io.provenance.invoice.util.extension.totalAmountI
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

object InvoiceValidator {
    fun validateInvoice(invoice: Invoice) {
        // Verify UUID is formatted correctly
        val invoiceUuid = invoice.invoiceUuid.toUuidOrNullI()
        checkNotNull(invoiceUuid) { "Invoice provided has an invalid invoiceUuid value [${invoice.invoiceUuid.value}]" }

        val errorPrefix = "Validation for invoice [$invoiceUuid] failed:"

        // Verify that addresses are properly formatted
        // TODO: Use some magic util to make sure these are actual provenance addresses unless test mode is on or something
        check(invoice.fromAddress.isNotBlank()) { "$errorPrefix The sender address must not be blank" }
        check(invoice.toAddress.isNotBlank()) { "$errorPrefix The receiver address must not be blank" }

        // Verify that created / due dates are properly formatted
        val today = LocalDate.now()
        val createdDate = invoice.invoiceCreatedDate.toLocalDateOrNullI().checkNotNullI { "$errorPrefix Created date was not provided" }
        check(createdDate.isBeforeInclusiveI(LocalDate.now())) { "$errorPrefix Created date [$createdDate] cannot be in the future [today: $today]" }
        invoice.invoiceDueDate.toLocalDateOrNullI().also { dueDate ->
            checkNotNull(dueDate) { "$errorPrefix Due date was not provided" }
            check(dueDate.isAfterInclusiveI(createdDate)) { "$errorPrefix Due date [$dueDate] cannot come before the created date [$createdDate]" }
        }

        // Verify that string qualifiers are defined
        check(invoice.description.isNotBlank()) { "$errorPrefix The description must not be blank" }
        check(invoice.paymentDenom in ExpectedDenom.ALL_EXPECTED_NAMES) { "$errorPrefix Unexpected denom: [${invoice.paymentDenom}]" }

        // Verify line items
        check(invoice.lineItemsList.isNotEmpty()) { "$errorPrefix Invoice contained no line items" }
        val invoiceTotal = invoice.totalAmountI()
        check(invoiceTotal > BigDecimal.ZERO) { "$errorPrefix Invoice charge sum [$invoiceTotal] must be greater than zero" }
        invoice.lineItemsList.forEach { validateLineItem(invoiceUuid, it) }
    }

    fun validateLineItem(invoiceUuid: UUID, lineItem: LineItem) {
        // Verify UUID is formatted correctly
        val lineUuid = lineItem.lineUuid.toUuidOrNullI()
        checkNotNull(lineUuid) { "Invoice [$invoiceUuid] Line Item with name [${lineItem.name}] and description [${lineItem.description}] had no uuid" }

        val errorPrefix = "Validation for invoice [$invoiceUuid] line [$lineUuid] w/ name [${lineItem.name}] failed:"

        // Verify that name is set
        check(lineItem.name.isNotBlank()) { "$errorPrefix The name must be provided" }
        check(lineItem.description.isNotBlank()) { "$errorPrefix The description must be provided" }
        check(lineItem.quantity > 0) { "$errorPrefix Each line item must have a quantity of at least one. Provided: [${lineItem.quantity}]" }
        lineItem.price.toBigDecimalOrNullI()
            .checkNotNullI { "$errorPrefix Each line item must have a defined price, but was [${lineItem.price.value}]" }
            .checkI({ it > BigDecimal.ZERO }) { "$errorPrefix Each line item must have a price greater than zero, but was [${lineItem.price.value}]" }
    }

    fun validateInvoiceForApproval(
        invoiceDto: InvoiceDto,
        objectStoreInvoice: Invoice,
        eventScopeId: String,
        eventTotalOwed: BigDecimal,
        eventInvoiceDenom: String,
    ) {
        val errorPrefix = "ORACLE APPROVAL VALIDATION [${invoiceDto.uuid}]:"
        check(invoiceDto.invoice.matches(objectStoreInvoice)) { "$errorPrefix DB invoice has mismatched fields with object store invoice" }
        validateInvoice(objectStoreInvoice)
        check(invoiceDto.writeScopeRequest.scopeIdI() == eventScopeId) { "$errorPrefix DB invoice scope id [${invoiceDto.writeScopeRequest.scopeIdI()}] did not match event scope id [$eventScopeId]" }
        check(invoiceDto.totalOwed == eventTotalOwed) { "$errorPrefix DB invoice total owed did not match event total owed [$eventTotalOwed]" }
        check(invoiceDto.invoice.paymentDenom == eventInvoiceDenom)
    }

    private fun Invoice.matches(that: Invoice): Boolean = this.invoiceUuid.value == that.invoiceUuid.value &&
        this.fromAddress == that.fromAddress &&
        this.toAddress == that.toAddress &&
        this.invoiceCreatedDate.value == that.invoiceCreatedDate.value &&
        this.invoiceDueDate.value == that.invoiceDueDate.value &&
        this.description == that.description &&
        this.paymentDenom == that.paymentDenom &&
        this.lineItemsCount == that.lineItemsCount &&
        this.lineItemsList.all { thisItem ->
            that.lineItemsList.singleOrNull { it.lineUuid.value == thisItem.lineUuid.value }
                ?.let { thatItem -> thisItem.matches(thatItem) } == true
        }

    private fun LineItem.matches(that: LineItem): Boolean = this.lineUuid.value == that.lineUuid.value &&
        this.name == that.name &&
        this.description == that.description &&
        this.quantity == that.quantity &&
        this.price.value == that.price.value
}
