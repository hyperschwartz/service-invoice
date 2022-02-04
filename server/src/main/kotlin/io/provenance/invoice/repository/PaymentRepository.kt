package io.provenance.invoice.repository

import io.provenance.invoice.domain.dto.PaymentDto
import io.provenance.invoice.domain.entities.PaymentRecord
import io.provenance.invoice.domain.exceptions.ResourceNotFoundException
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Repository
class PaymentRepository {
    fun findByUuidOrNull(uuid: UUID): PaymentDto? = transaction { PaymentRecord.findById(uuid)?.toDto() }
    fun findByUuid(uuid: UUID): PaymentDto = findByUuidOrNull(uuid)
        ?: throw ResourceNotFoundException("Unable find payment by uuid [$uuid]")

    fun insert(
        invoiceUuid: UUID,
        paymentTime: OffsetDateTime,
        fromAddress: String,
        toAddress: String,
        paymentAmount: BigDecimal,
        paymentUuid: UUID = UUID.randomUUID(),
    ): PaymentDto = transaction {
        PaymentRecord.insert(
            invoiceUuid = invoiceUuid,
            paymentTime = paymentTime,
            fromAddress = fromAddress,
            toAddress = toAddress,
            paymentAmount = paymentAmount,
            paymentUuid = paymentUuid,
        ).toDto()
    }

    fun findAllByInvoiceUuid(invoiceUuid: UUID): List<PaymentDto> = transaction {
        PaymentRecord.findAllByInvoiceUuid(invoiceUuid)
    }
}
