package tech.figure.invoice.util.validation

import tech.figure.invoice.InvoiceProtos.Invoice
import tech.figure.invoice.InvoiceProtos.LineItem
import tech.figure.invoice.util.enums.ExpectedDenom
import tech.figure.invoice.util.extension.check
import tech.figure.invoice.util.extension.checkNotNull
import tech.figure.invoice.util.extension.isAfterInclusive
import tech.figure.invoice.util.extension.isBeforeInclusive
import tech.figure.invoice.util.extension.toBigDecimalOrNull
import tech.figure.invoice.util.extension.toLocalDateOrNull
import tech.figure.invoice.util.extension.toUuidOrNull
import tech.figure.invoice.util.extension.totalAmount
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

object InvoiceValidator {
    fun validateInvoice(invoice: Invoice) {
        // Verify UUID is formatted correctly
        val invoiceUuid = invoice.invoiceUuid.toUuidOrNull()
        checkNotNull(invoiceUuid) { "Invoice provided has an invalid invoiceUuid value [${invoice.invoiceUuid.value}]" }

        val errorPrefix = "Validation for invoice [$invoiceUuid] failed:"

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

    fun validateLineItem(invoiceUuid: UUID, lineItem: LineItem) {
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