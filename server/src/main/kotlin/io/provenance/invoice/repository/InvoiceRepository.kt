package io.provenance.invoice.repository

import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.domain.dto.InvoiceDto
import io.provenance.invoice.domain.entities.InvoiceRecord
import io.provenance.invoice.domain.entities.InvoiceUpdateQueryParam
import io.provenance.invoice.domain.entities.InvoiceWritesResponse
import io.provenance.invoice.domain.exceptions.ResourceNotFoundException
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Repository
import io.provenance.invoice.util.enums.InvoiceStatus
import io.provenance.invoice.util.extension.wrapListI
import io.provenance.metadata.v1.MsgWriteRecordRequest
import io.provenance.metadata.v1.MsgWriteScopeRequest
import io.provenance.metadata.v1.MsgWriteSessionRequest
import java.util.UUID

@Repository
class InvoiceRepository {
    fun findByUuidOrNull(uuid: UUID): Invoice? = transaction { InvoiceRecord.findById(uuid)?.invoice }

    fun findByUuid(uuid: UUID): Invoice = findByUuidOrNull(uuid)
        ?: throw ResourceNotFoundException("Failed to find invoice by uuid [$uuid]")

    fun findDtoByUuidOrNull(uuid: UUID): InvoiceDto? = transaction { InvoiceRecord.findById(uuid)?.toDto() }

    fun findDtoByUuid(uuid: UUID): InvoiceDto = findDtoByUuidOrNull(uuid)
        ?: throw ResourceNotFoundException("Failed to find invoice dto by uuid [$uuid]")

    fun insert(
        invoice: Invoice,
        status: InvoiceStatus,
        writeScopeRequest: MsgWriteScopeRequest,
        writeSessionRequest: MsgWriteSessionRequest,
        writeRecordRequest: MsgWriteRecordRequest,
    ): InvoiceDto = transaction {
        InvoiceRecord.insert(
            invoice = invoice,
            status = status,
            writeScopeRequest = writeScopeRequest,
            writeSessionRequest = writeSessionRequest,
            writeRecordRequest = writeRecordRequest,
        ).toDto()
    }

    fun update(
        invoice: Invoice,
        status: InvoiceStatus? = null,
        processingStatus: InvoiceStatus? = null,
        writeScopeRequest: MsgWriteScopeRequest? = null,
        writeSessionRequest: MsgWriteSessionRequest? = null,
        writeRecordRequest: MsgWriteRecordRequest? = null,
    ): InvoiceDto = transaction {
        InvoiceRecord.update(
            invoiceParam = InvoiceUpdateQueryParam.InvoiceProto(invoice),
            status = status,
            writeScopeRequest = writeScopeRequest,
            writeSessionRequest = writeSessionRequest,
            writeRecordRequest = writeRecordRequest,
        ).toDto()
    }

    fun update(
        uuid: UUID,
        status: InvoiceStatus? = null,
        processingStatus: InvoiceStatus? = null,
        writeScopeRequest: MsgWriteScopeRequest? = null,
        writeSessionRequest: MsgWriteSessionRequest? = null,
        writeRecordRequest: MsgWriteRecordRequest? = null,
    ): InvoiceDto = transaction {
        InvoiceRecord.update(
            invoiceParam = InvoiceUpdateQueryParam.InvoiceUuid(uuid),
            status = status,
            writeScopeRequest = writeScopeRequest,
            writeSessionRequest = writeSessionRequest,
            writeRecordRequest = writeRecordRequest,
        ).toDto()
    }

    fun findAllByFromAddress(fromAddress: String): List<InvoiceDto> =
        transaction { InvoiceRecord.findAllFromAddress(fromAddress) }

    fun findAllByToAddress(toAddress: String): List<InvoiceDto> =
        transaction { InvoiceRecord.findAllToAddresses(toAddress.wrapListI()) }

    fun findAllByToAddresses(toAddresses: Collection<String>): List<InvoiceDto> =
        transaction { InvoiceRecord.findAllToAddresses(toAddresses) }

    fun findInvoiceUuidsWithFailedOracleApprovals(onlyIncludeUuids: Collection<UUID>? = null): List<UUID> =
        transaction { InvoiceRecord.findInvoiceUuidsWithFailedOracleApprovals(onlyIncludeUuids) }

    fun findWritesOrNull(invoiceUuid: UUID): InvoiceWritesResponse? =
        transaction { InvoiceRecord.findWritesOrNull(invoiceUuid) }

    fun findWriteScopeRequestOrNull(invoiceUuid: UUID): MsgWriteScopeRequest? =
        transaction { InvoiceRecord.findWriteScopeRequestOrNull(invoiceUuid) }

    fun findWriteSessionRequestOrNull(invoiceUuid: UUID): MsgWriteSessionRequest? =
        transaction { InvoiceRecord.findWriteSessionRequestOrNull(invoiceUuid) }

    fun findWriteRecordRequestOrNull(invoiceUuid: UUID): MsgWriteRecordRequest? =
        transaction { InvoiceRecord.findWriteRecordRequestOrNull(invoiceUuid) }
}
