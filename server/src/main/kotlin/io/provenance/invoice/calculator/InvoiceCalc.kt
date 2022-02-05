package io.provenance.invoice.calculator

import io.provenance.invoice.util.enums.InvoiceStatus
import io.provenance.invoice.util.enums.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class InvoiceCalc(
    val uuid: UUID,
    val calcTime: OffsetDateTime,
    val invoiceStatus: InvoiceStatus,
    val paymentStatus: PaymentStatus,
    val ownerAddress: String,
    val payerAddress: String,
    val createdDate: LocalDate,
    val dueDate: LocalDate,
    val description: String,
    val lineItems: List<LineItemCalc>,
    val payments: List<PaymentCalc>,
    val paymentSum: BigDecimal,
    val paymentDenom: String,
    val originalOwed: BigDecimal,
    val remainingOwed: BigDecimal,
    val paymentDelinquentDays: Int,
    val payoffTime: OffsetDateTime?,
) {
    companion object {
        val PAYOFF_STATUSES: Set<PaymentStatus> = setOf(PaymentStatus.PAID_ON_TIME, PaymentStatus.PAID_LATE)
    }

    val isPaidOff: Boolean by lazy { paymentStatus in PAYOFF_STATUSES }
}
