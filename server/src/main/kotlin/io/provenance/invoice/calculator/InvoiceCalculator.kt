package io.provenance.invoice.calculator

import io.provenance.invoice.domain.dto.InvoiceDto
import io.provenance.invoice.domain.dto.PaymentDto
import io.provenance.invoice.util.enums.InvoiceStatus
import io.provenance.invoice.util.enums.PaymentStatus
import io.provenance.invoice.util.extension.daysBetweenI
import io.provenance.invoice.util.extension.isBeforeInclusiveI
import io.provenance.invoice.util.extension.toLocalDateI
import mu.KLogging
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * Simple tool that calculates various totals owed on an invoice based on payments made and the calculation time.
 */
class InvoiceCalculator(
    val calcTime: OffsetDateTime,
    val invoiceDto: InvoiceDto,
    val payments: List<PaymentDto>
) {
    private companion object : KLogging()

    val calc: InvoiceCalc by lazy { genCalc() }

    private val dueDate: LocalDate = invoiceDto.invoice.invoiceDueDate.toLocalDateI()

    private fun genCalc(): InvoiceCalc {
        val applicablePayments = payments.filter { it.effectiveTime.isBeforeInclusiveI(calcTime) }
        var payoffTime: OffsetDateTime? = null
        var currentOwed = invoiceDto.totalOwed
        val paymentCalcs = applicablePayments.map { paymentDto ->
            genPaymentCalc(currentOwed = currentOwed, payment = paymentDto).also { calc ->
                currentOwed -= calc.owedAfterPayment
                if (currentOwed <= BigDecimal.ZERO) {
                    // Only set payoff time if it hasn't yet been set.  Payments after payoff should not affect this time
                    payoffTime = payoffTime ?: paymentDto.effectiveTime
                    if (currentOwed < BigDecimal.ZERO) {
                        logger.error("Payment [${paymentDto.uuid}] overpaid the invoice down to [${calc.owedAfterPayment}]")
                    }
                }
            }
        }
        return InvoiceCalc(
            uuid = invoiceDto.uuid,
            calcTime = calcTime,
            invoiceStatus = invoiceDto.status,
            paymentStatus = calcPaymentStatus(payoffTime, currentOwed),
            ownerAddress = invoiceDto.invoice.fromAddress,
            createdDate = invoiceDto.invoice.invoiceCreatedDate.toLocalDateI(),
            dueDate = invoiceDto.invoice.invoiceDueDate.toLocalDateI(),
            description = invoiceDto.invoice.description,
            payments = paymentCalcs,
            paymentSum = paymentCalcs.sumOf { it.paymentAmount },
            originalOwed = invoiceDto.totalOwed,
            remainingOwed = currentOwed,
            paymentDelinquentDays = calcTime.toLocalDate().daysBetweenI(dueDate).coerceAtLeast(0),
            payoffTime = payoffTime,
        )
    }

    private fun genPaymentCalc(currentOwed: BigDecimal, payment: PaymentDto): PaymentCalc = PaymentCalc(
        uuid = payment.uuid,
        invoiceUuid = payment.invoiceUuid,
        effectiveTime = payment.effectiveTime,
        paymentAmount = payment.paymentAmount,
        paymentDenom = invoiceDto.invoice.paymentDenom,
        fromAddress = payment.fromAddress,
        owedBeforePayment = currentOwed,
        owedAfterPayment = currentOwed - payment.paymentAmount,
    )

    private fun calcPaymentStatus(
        payoffTime: OffsetDateTime?,
        currentOwed: BigDecimal
    ): PaymentStatus {
        // Payments should not be allowed when the invoice has not yet been approved by the oracle
        if (invoiceDto.status != InvoiceStatus.APPROVED) {
            return PaymentStatus.RESTRICTED
        }
        // If the invoice still has an amount owed, it should return some form of repayment status
        if (currentOwed > BigDecimal.ZERO) {
            return if (calcTime.toLocalDate().isBeforeInclusiveI(dueDate)) {
                PaymentStatus.REPAY_PERIOD
            } else {
                PaymentStatus.DELINQUENT
            }
        }
        // Otherwise, the invoice has been paid off and it's based on the payoff time value
        return if (payoffTime?.toLocalDate()?.isBeforeInclusiveI(dueDate) == true) {
            PaymentStatus.PAID_ON_TIME
        } else {
            PaymentStatus.PAID_LATE
        }
    }

}
