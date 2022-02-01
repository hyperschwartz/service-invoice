package io.provenance.invoice.domain.dto

import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.domain.entities.InvoiceRecord
import io.provenance.invoice.util.enums.InvoiceProcessingStatus
import io.provenance.invoice.util.extension.totalAmount
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

// TODO: Add current owed based on received values from event stream
data class InvoiceDto(
    val uuid: UUID,
    val invoice: Invoice,
    val processingStatus: InvoiceProcessingStatus,
    val markerDenom: String,
    val markerAddress: String,
    val totalOwed: BigDecimal,
    val created: OffsetDateTime,
    val updated: OffsetDateTime?,
) {
    companion object {
        fun fromRecord(record: InvoiceRecord): InvoiceDto = InvoiceDto(
            uuid = record.invoiceUuid,
            invoice = record.invoice,
            processingStatus = record.processingStatus,
            markerDenom = record.markerDenom,
            markerAddress = record.markerAddress,
            totalOwed = record.invoice.totalAmount(),
            created = record.createdTime,
            updated = record.updatedTime,
        )
    }
}
