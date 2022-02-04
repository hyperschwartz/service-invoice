package io.provenance.invoice.domain.dto

import io.provenance.invoice.domain.entities.PaymentRecord
import io.provenance.invoice.domain.entities.PaymentTable
import org.jetbrains.exposed.sql.ResultRow
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class PaymentDto(
    val uuid: UUID,
    val invoiceUuid: UUID,
    val effectiveTime: OffsetDateTime,
    val fromAddress: String,
    val toAddress: String,
    val paymentAmount: BigDecimal,
    val createdTime: OffsetDateTime,
    val updatedTime: OffsetDateTime?
) {
    companion object {
        fun fromRecord(record: PaymentRecord): PaymentDto = PaymentDto(
            uuid = record.paymentUuid,
            invoiceUuid = record.invoiceUuid,
            effectiveTime = record.paymentTime,
            fromAddress = record.fromAddress,
            toAddress = record.toAddress,
            paymentAmount = record.paymentAmount,
            createdTime = record.createdTime,
            updatedTime = record.updatedTime,
        )

        fun fromResultRow(resultRow: ResultRow): PaymentDto = PaymentDto(
            uuid = resultRow[PaymentTable.paymentUuid],
            invoiceUuid = resultRow[PaymentTable.invoiceUuid],
            effectiveTime = resultRow[PaymentTable.paymentTime],
            fromAddress = resultRow[PaymentTable.fromAddress],
            toAddress = resultRow[PaymentTable.toAddress],
            paymentAmount = resultRow[PaymentTable.paymentAmount],
            createdTime = resultRow[PaymentTable.createdTime],
            updatedTime = resultRow[PaymentTable.updatedTime],
        )
    }
}
