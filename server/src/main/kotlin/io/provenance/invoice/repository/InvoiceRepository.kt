package io.provenance.invoice.repository

import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.domain.dto.InvoiceDto
import io.provenance.invoice.domain.entities.InvoiceRecord
import io.provenance.invoice.domain.entities.InvoiceUpdateQueryParam
import io.provenance.invoice.domain.exceptions.ResourceNotFoundException
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Repository
import io.provenance.invoice.util.enums.InvoiceProcessingStatus
import io.provenance.metadata.v1.MsgWriteRecordRequest
import io.provenance.metadata.v1.MsgWriteScopeRequest
import io.provenance.metadata.v1.MsgWriteSessionRequest
import java.util.UUID

@Repository
class InvoiceRepository {
    fun findByUuidOrNull(uuid: UUID): Invoice? = transaction { InvoiceRecord.findById(uuid)?.invoice }

    fun findByUuid(uuid: UUID): Invoice = findByUuidOrNull(uuid)
        ?: throw ResourceNotFoundException("Failed to find invoice by uuid [$uuid]")

    fun insert(
        invoice: Invoice,
        status: InvoiceProcessingStatus,
        markerDenom: String,
        markerAddress: String,
        writeScopeRequest: MsgWriteScopeRequest,
        writeSessionRequest: MsgWriteSessionRequest,
        writeRecordRequest: MsgWriteRecordRequest,
    ): InvoiceDto = transaction {
        InvoiceRecord.insert(
            invoice = invoice,
            status = status,
            markerDenom = markerDenom,
            markerAddress = markerAddress,
            writeScopeRequest = writeScopeRequest,
            writeSessionRequest = writeSessionRequest,
            writeRecordRequest = writeRecordRequest,
        ).toDto()
    }

    fun update(
        invoice: Invoice,
        status: InvoiceProcessingStatus? = null,
        processingStatus: InvoiceProcessingStatus? = null,
        markerDenom: String? = null,
        markerAddress: String? = null,
        writeScopeRequest: MsgWriteScopeRequest? = null,
        writeSessionRequest: MsgWriteSessionRequest? = null,
        writeRecordRequest: MsgWriteRecordRequest? = null,
    ): InvoiceDto = transaction {
        InvoiceRecord.update(
            invoiceParam = InvoiceUpdateQueryParam.InvoiceProto(invoice),
            status = status,
            markerDenom = markerDenom,
            markerAddress = markerAddress,
            writeScopeRequest = writeScopeRequest,
            writeSessionRequest = writeSessionRequest,
            writeRecordRequest = writeRecordRequest,
        ).toDto()
    }

    fun update(
        uuid: UUID,
        status: InvoiceProcessingStatus? = null,
        processingStatus: InvoiceProcessingStatus? = null,
        markerDenom: String? = null,
        markerAddress: String? = null,
        writeScopeRequest: MsgWriteScopeRequest? = null,
        writeSessionRequest: MsgWriteSessionRequest? = null,
        writeRecordRequest: MsgWriteRecordRequest? = null,
    ): InvoiceDto = transaction {
        InvoiceRecord.update(
            invoiceParam = InvoiceUpdateQueryParam.InvoiceUuid(uuid),
            status = status,
            markerDenom = markerDenom,
            markerAddress = markerAddress,
            writeScopeRequest = writeScopeRequest,
            writeSessionRequest = writeSessionRequest,
            writeRecordRequest = writeRecordRequest,
        ).toDto()
    }

    fun findAllByFromAddress(fromAddress: String): List<Invoice> =
        transaction { InvoiceRecord.findAllFromAddress(fromAddress) }

    fun findAllByToAddress(toAddress: String): List<Invoice> =
        transaction { InvoiceRecord.findAllToAddress(toAddress) }
}
