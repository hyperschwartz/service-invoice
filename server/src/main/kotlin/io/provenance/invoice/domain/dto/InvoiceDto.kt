package io.provenance.invoice.domain.dto

import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.domain.entities.InvoiceRecord
import io.provenance.invoice.util.enums.InvoiceProcessingStatus
import io.provenance.invoice.util.extension.totalAmount
import io.provenance.metadata.v1.MsgWriteRecordRequest
import io.provenance.metadata.v1.MsgWriteScopeRequest
import io.provenance.metadata.v1.MsgWriteSessionRequest
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

// TODO: Add current owed based on received values from event stream
data class InvoiceDto(
    val uuid: UUID,
    val invoice: Invoice,
    val processingStatus: InvoiceProcessingStatus,
    val totalOwed: BigDecimal,
    val writeScopeRequest: MsgWriteScopeRequest,
    val writeSessionRequest: MsgWriteSessionRequest,
    val writeRecordRequest: MsgWriteRecordRequest,
    val created: OffsetDateTime,
    val updated: OffsetDateTime?,
) {
    companion object {
        fun fromRecord(record: InvoiceRecord): InvoiceDto = InvoiceDto(
            uuid = record.invoiceUuid,
            invoice = record.invoice,
            processingStatus = record.processingStatus,
            totalOwed = record.invoice.totalAmount(),
            writeScopeRequest = record.writeScopeRequest,
            writeSessionRequest = record.writeSessionRequest,
            writeRecordRequest = record.writeRecordRequest,
            created = record.createdTime,
            updated = record.updatedTime,
        )
    }
}
