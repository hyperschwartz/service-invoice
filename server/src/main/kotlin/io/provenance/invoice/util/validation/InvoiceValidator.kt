package io.provenance.invoice.util.validation

import arrow.core.Validated
import arrow.core.andThen
import arrow.core.invalid
import arrow.core.valid
import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.InvoiceProtos.LineItem
import io.provenance.invoice.domain.dto.InvoiceDto
import io.provenance.invoice.util.enums.ExpectedDenom
import io.provenance.invoice.util.extension.elvisI
import io.provenance.invoice.util.extension.isAfterInclusiveI
import io.provenance.invoice.util.extension.isBeforeInclusiveI
import io.provenance.invoice.util.extension.scopeIdI
import io.provenance.invoice.util.extension.toBigDecimalOrNullI
import io.provenance.invoice.util.extension.toLocalDateOrNullI
import io.provenance.invoice.util.extension.toUuidOrNullI
import io.provenance.invoice.util.extension.totalAmountI
import io.provenance.invoice.util.provenance.ProvenanceUtil
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

sealed class InvoiceValidationError(override val fieldName: String, override val errorMessage: String) : ArrowValidationError {
    // Top-level invoice errors
    class InvalidInvoiceUuid(value: String) : InvoiceValidationError("invoiceUuid", "Non-uuid value provided: [$value]")
    object InvalidInvoiceFromAddress : InvoiceValidationError("fromAddress", "Must be in Bech32 format")
    object InvalidInvoiceToAddress : InvoiceValidationError("toAddress", "Must be populated")
    class InvalidInvoiceCreatedDate(value: String) : InvoiceValidationError("createdDate", "Non-date value provided: [$value]")
    object FutureInvoiceCreatedDate : InvoiceValidationError("createdDate", "Must not be in the future")
    class InvalidInvoiceDueDate(value: String) : InvoiceValidationError("dueDate", "Non-date value provided: [$value]")
    class InvoiceDueDateBeforeCreatedDate(createdDate: LocalDate, dueDate: LocalDate) : InvoiceValidationError("dueDate", "Must not come before created date: [$createdDate] > [$dueDate]")
    object InvalidInvoiceDescription : InvoiceValidationError("description", "Must be populated")
    class InvalidInvoiceDenom(value: String) : InvoiceValidationError("paymentDenom", "Unexpected value provided: [$value]")
    object NoLineItems : InvoiceValidationError("lineItems", "At least one must be present")
    class InvalidLineItemTotal(total: BigDecimal) : InvoiceValidationError("lineItems", "Total must be greater than zero, but was [$total]")

    // Line item errors
    class InvalidLineUuid(value: String) : InvoiceValidationError("lineUuid", "Non-uuid value provided: [$value]")
    object InvalidLineName : InvoiceValidationError("name", "Must be populated")
    object InvalidLineDescription : InvoiceValidationError("description", "Must be populated")
    class InvalidLineQuantity(quantity: Int) : InvoiceValidationError("quantity", "Must have at least 1, but was [$quantity]")
    class InvalidLinePrice(value: String) : InvoiceValidationError("price", "Must be a valid number, but was [$value]")
    class LinePriceTooLow(price: BigDecimal) : InvoiceValidationError("price", "Must be greater than zero, but was [$price]")
}

class ValidatedInvoice internal constructor(
    private val invoice: Invoice,
    private val now: LocalDate = LocalDate.now()
) : ArrowValidator<InvoiceValidationError>() {
    companion object {
        fun new(invoice: Invoice): ValidatedInvoice = ValidatedInvoice(invoice)
    }

    override fun getFailurePrefix(): String = "Validation for invoice [${invoice.invoiceUuid.value}] failed"

    val uuid: Validated<InvoiceValidationError, UUID> = invoice
        .invoiceUuid
        .toUuidOrNullI()
        ?.valid()
        .elvisI { InvoiceValidationError.InvalidInvoiceUuid(invoice.invoiceUuid.value).invalid() }
        .bindValidation()

    val fromAddress: Validated<InvoiceValidationError, String> = invoice
        .fromAddress
        .takeIf(ProvenanceUtil::isBech32AddressValid)
        ?.valid()
        .elvisI { InvoiceValidationError.InvalidInvoiceFromAddress.invalid() }
        .bindValidation()

    val toAddress: Validated<InvoiceValidationError, String> = invoice
        .toAddress
        .takeIf { it.isNotBlank() }
        ?.valid()
        .elvisI { InvoiceValidationError.InvalidInvoiceToAddress.invalid() }
        .bindValidation()

    val createdDate: Validated<InvoiceValidationError, LocalDate> = invoice
        .invoiceCreatedDate
        .toLocalDateOrNullI()
        ?.valid()
        .elvisI { InvoiceValidationError.InvalidInvoiceCreatedDate(invoice.invoiceCreatedDate.value).invalid() }
        .andThen { createdDate ->
            createdDate.takeIf { it.isBeforeInclusiveI(now) }?.valid()
                ?: InvoiceValidationError.FutureInvoiceCreatedDate.invalid()
        }
        .bindValidation()

    val dueDate: Validated<InvoiceValidationError, LocalDate> = invoice
        .invoiceDueDate
        .toLocalDateOrNullI()
        ?.valid()
        .elvisI { InvoiceValidationError.InvalidInvoiceDueDate(invoice.invoiceDueDate.value).invalid() }
        .andThen { dueDate ->
            createdDate.fold(
                // If created date was invalid and not derived as a LocalDate, then no extra validation can occur
                fe = { dueDate.valid() },
                // Otherwise, we can check to see if the created date > due date
                fa = { createdDate ->
                    dueDate.takeIf { it.isAfterInclusiveI(createdDate) }?.valid()
                        ?: InvoiceValidationError.InvoiceDueDateBeforeCreatedDate(createdDate, dueDate).invalid()
                }
            )
        }
        .bindValidation()

    val description: Validated<InvoiceValidationError, String> = invoice
        .description
        .takeIf { it.isNotBlank() }
        ?.valid()
        .elvisI { InvoiceValidationError.InvalidInvoiceDescription.invalid() }
        .bindValidation()

    val paymentDenom: Validated<InvoiceValidationError, String> = invoice
        .paymentDenom
        .takeIf { it in ExpectedDenom.ALL_EXPECTED_NAMES }
        ?.valid()
        .elvisI { InvoiceValidationError.InvalidInvoiceDenom(invoice.paymentDenom).invalid() }
        .bindValidation()

    val lineItemCheck: Validated<InvoiceValidationError, List<LineItem>> = invoice
        .lineItemsList
        .takeIf { it.isNotEmpty() }
        ?.valid()
        .elvisI { InvoiceValidationError.NoLineItems.invalid() }
        .andThen { lineItems ->
            val invoiceTotal = invoice.totalAmountI()
            lineItems.takeIf { invoiceTotal > BigDecimal.ZERO }?.valid()
                ?: InvoiceValidationError.InvalidLineItemTotal(invoiceTotal).invalid()
        }
        .bindValidation()

    val validatedLineItems: List<ValidatedLineItem> = invoice
        .lineItemsList
        .map(::ValidatedLineItem)
        .onEach { validatedLineItem -> bindValidationsFrom(validatedLineItem) }
}

class ValidatedLineItem internal constructor(private val lineItem: LineItem) : ArrowValidator<InvoiceValidationError>() {

    companion object {
        fun new(lineItem: LineItem): ValidatedLineItem = ValidatedLineItem(lineItem)
    }

    override fun getFailurePrefix(): String = "Validation for line item [${lineItem.lineUuid.value}] failed"

    val uuid: Validated<InvoiceValidationError, UUID> = lineItem
        .lineUuid
        .toUuidOrNullI()
        ?.valid()
        .elvisI { InvoiceValidationError.InvalidLineUuid(lineItem.lineUuid.value).invalid() }
        .bindValidation()

    val name: Validated<InvoiceValidationError, String> = lineItem
        .name
        .takeIf { it.isNotBlank() }
        ?.valid()
        .elvisI { InvoiceValidationError.InvalidLineName.invalid() }
        .bindValidation()

    val description: Validated<InvoiceValidationError, String> = lineItem
        .description
        .takeIf { it.isNotBlank() }
        ?.valid()
        .elvisI { InvoiceValidationError.InvalidLineDescription.invalid() }
        .bindValidation()

    val quantity: Validated<InvoiceValidationError, Int> = lineItem
        .quantity
        .takeIf { it > 0 }
        ?.valid()
        .elvisI { InvoiceValidationError.InvalidLineQuantity(lineItem.quantity).invalid() }
        .bindValidation()

    val price: Validated<InvoiceValidationError, BigDecimal> = lineItem
        .price
        .toBigDecimalOrNullI()
        ?.valid()
        .elvisI { InvoiceValidationError.InvalidLinePrice(lineItem.price.value).invalid() }
        .andThen { price ->
            price.takeIf { it > BigDecimal.ZERO }?.valid()
                ?: InvoiceValidationError.LinePriceTooLow(price).invalid()
        }
        .bindValidation()
}

object InvoiceValidator {
    fun validateInvoiceForApproval(
        invoiceDto: InvoiceDto,
        objectStoreInvoice: Invoice,
        eventScopeId: String,
        eventTotalOwed: BigDecimal,
        eventInvoiceDenom: String,
    ) {
        val errorPrefix = "ORACLE APPROVAL VALIDATION [${invoiceDto.uuid}]:"
        check(invoiceDto.invoice.matches(objectStoreInvoice)) { "$errorPrefix DB invoice has mismatched fields with object store invoice" }
        ValidatedInvoice.new(objectStoreInvoice).generateValidationReport().throwFailures()
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
