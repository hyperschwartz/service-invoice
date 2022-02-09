package io.provenance.invoice.factory

import io.provenance.invoice.calculator.InvoiceCalc
import io.provenance.invoice.calculator.InvoiceCalculator
import io.provenance.invoice.domain.dto.InvoiceDto
import io.provenance.invoice.domain.entities.PaymentTable.invoiceUuid
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
    fun emptyCalc(invoiceDto: InvoiceDto, calcTime: OffsetDateTime = OffsetDateTime.now()): InvoiceCalc = InvoiceCalculator(calcTime, invoiceDto, listOf()).calc

    fun generate(
        invoiceUuid: UUID,
        calcTime: OffsetDateTime = OffsetDateTime.now(),
    ): InvoiceCalc = InvoiceCalculator(
        calcTime = calcTime,
        invoiceDto = invoiceRepository.findDtoByUuid(invoiceUuid),
        payments = paymentRepository.findAllByInvoiceUuid(invoiceUuid),
    ).calc

    fun generateMany(
        invoiceDtos: List<InvoiceDto>,
        calcTime: OffsetDateTime = OffsetDateTime.now(),
    ): List<InvoiceDto> {
        val invoiceDtoLookup = invoiceDtos.associate { it.uuid to it }

        val calcMap = paymentRepository.findAllByInvoiceUuids(invoiceDtos.map { it.uuid }).groupBy { it.invoiceUuid }
            .mapValues { (invoiceUuid, payments) -> InvoiceCalculator(
                calcTime = calcTime,
                invoiceDto = invoiceDtoLookup[invoiceUuid]!!,
                payments = payments
            ).calc }

        return invoiceDtos.map {
            it.withCalc(calcMap[it.uuid] ?: emptyCalc(it, calcTime))
        }
    }
}
