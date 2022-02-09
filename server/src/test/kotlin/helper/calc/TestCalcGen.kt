package helper.calc

import io.provenance.invoice.calculator.InvoiceCalc
import io.provenance.invoice.calculator.InvoiceCalculator
import io.provenance.invoice.domain.dto.InvoiceDto
import io.provenance.invoice.domain.dto.PaymentDto
import io.provenance.invoice.util.enums.InvoiceStatus
import io.provenance.invoice.util.validation.InvoiceValidator
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class TestCalcGen internal constructor(
    val invoiceDto: InvoiceDto,
    val payments: List<PaymentDto>
) {
    companion object {
        fun fromTestInvoice(
            testInvoice: TestInvoice,
            startingInvoiceStatus: InvoiceStatus = InvoiceStatus.APPROVED,
        ): TestCalcGen {
            val invoiceDto = testInvoice.toDto(startingInvoiceStatus)
            // Ensure invoices added to the TestCalcGen are always valid, preventing bad test code from running
            InvoiceValidator.validateInvoice(invoiceDto.invoice)
            return TestCalcGen(testInvoice.toDto(startingInvoiceStatus), emptyList())
        }
    }

    fun changeInvoiceStatus(newStatus: InvoiceStatus): TestCalcGen = copy(
        invoiceDto.copy(status = newStatus),
        payments = payments,
    )

    fun addPayment(
        paymentAmount: BigDecimal,
        uuid: UUID = UUID.randomUUID(),
        effectiveTime: OffsetDateTime = OffsetDateTime.now(),
        fromAddress: String = invoiceDto.invoice.toAddress,
        toAddress: String = invoiceDto.invoice.fromAddress,
    ): TestCalcGen {
        val calc = genCalc(effectiveTime)
        check(calc.remainingOwed >= paymentAmount) { "Cannot add a payment that would overpay the invoice" }
        return copy(
            invoiceDto = invoiceDto,
            payments = payments + PaymentDto(
                uuid = uuid,
                invoiceUuid = invoiceDto.uuid,
                effectiveTime = effectiveTime,
                fromAddress = fromAddress,
                toAddress = toAddress,
                paymentAmount = paymentAmount,
                createdTime = effectiveTime,
                updatedTime = null,
            )
        )
    }

    fun genCalc(calcTime: OffsetDateTime = OffsetDateTime.now()): InvoiceCalc = InvoiceCalculator(
        calcTime = calcTime,
        invoiceDto = invoiceDto,
        payments = payments,
    ).calc
}