package io.provenance.invoice.factory

import io.provenance.invoice.calculator.InvoiceCalc
import io.provenance.invoice.calculator.InvoiceCalculator
import io.provenance.invoice.repository.InvoiceRepository
import io.provenance.invoice.repository.PaymentRepository
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.UUID

@Component
class InvoiceCalcFactory(
    private val invoiceRepository: InvoiceRepository,
    private val paymentRepository: PaymentRepository
) {
    fun generate(
        invoiceUuid: UUID,
        calcTime: OffsetDateTime = OffsetDateTime.now(),
    ): InvoiceCalc = InvoiceCalculator(
        calcTime = calcTime,
        invoiceDto = invoiceRepository.findDtoByUuid(invoiceUuid),
        payments = paymentRepository.findAllByInvoiceUuid(invoiceUuid),
    ).calc
}
