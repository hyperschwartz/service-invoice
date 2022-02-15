package io.provenance.invoice.domain.entities

import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.domain.dto.InvoiceDto
import io.provenance.invoice.domain.exceptions.ResourceNotFoundException
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.select
import io.provenance.invoice.util.enums.InvoiceStatus
import io.provenance.invoice.util.exposed.offsetDatetime
import io.provenance.invoice.util.exposed.proto
import io.provenance.invoice.util.extension.toUuidI
import io.provenance.metadata.v1.MsgWriteRecordRequest
import io.provenance.metadata.v1.MsgWriteScopeRequest
import io.provenance.metadata.v1.MsgWriteSessionRequest
import org.jetbrains.exposed.sql.and
import java.time.OffsetDateTime
import java.util.UUID

object InvoiceTable : UUIDTable(columnName = "invoice_uuid", name = "invoice") {
    val data = proto(name = "data", Invoice.getDefaultInstance())
    val fromAddress = text(name = "from_address")
    val toAddress = text(name = "to_address")
    val status = text(name = "status")
    val writeScopeRequest = proto(name = "write_scope_request", MsgWriteScopeRequest.getDefaultInstance())
    val writeSessionRequest = proto(name = "write_session_request", MsgWriteSessionRequest.getDefaultInstance())
    val writeRecordRequest = proto(name = "write_record_request", MsgWriteRecordRequest.getDefaultInstance())
    val createdTime = offsetDatetime(name = "created_time")
    val updatedTime = offsetDatetime(name = "updated_time").nullable()
}

open class InvoiceEntityClass(invoiceTable: InvoiceTable): UUIDEntityClass<InvoiceRecord>(invoiceTable) {
    fun insert(
        invoice: Invoice,
        status: InvoiceStatus,
        writeScopeRequest: MsgWriteScopeRequest,
        writeSessionRequest: MsgWriteSessionRequest,
        writeRecordRequest: MsgWriteRecordRequest,
        created: OffsetDateTime = OffsetDateTime.now()
    ): InvoiceRecord = findById(invoice.invoiceUuid.toUuidI())
        ?.also { throw IllegalStateException("Invoice [${invoice.invoiceUuid.value}] already exists in the database") }
        .run {
            new(invoice.invoiceUuid.toUuidI()) {
                this.data = invoice
                this.fromAddress = invoice.fromAddress
                this.toAddress = invoice.toAddress
                this.status = status.name
                this.writeScopeRequest = writeScopeRequest
                this.writeSessionRequest = writeSessionRequest
                this.writeRecordRequest = writeRecordRequest
                this.createdTime = created
            }
        }

    fun update(
        invoiceParam: InvoiceUpdateQueryParam,
        status: InvoiceStatus? = null,
        writeScopeRequest: MsgWriteScopeRequest? = null,
        writeSessionRequest: MsgWriteSessionRequest? = null,
        writeRecordRequest: MsgWriteRecordRequest? = null,
        updateTime: OffsetDateTime = OffsetDateTime.now(),
    ): InvoiceRecord = findById(invoiceParam.invoiceUuid())?.apply {
        if (invoiceParam is InvoiceUpdateQueryParam.InvoiceProto) {
            this.data = invoiceParam.proto
            this.fromAddress = invoiceParam.proto.fromAddress
            this.toAddress = invoiceParam.proto.toAddress
        }
        this.updatedTime = updateTime
        status?.name?.also { this.status = it }
        writeScopeRequest?.also { this.writeScopeRequest = it }
        writeSessionRequest?.also { this.writeSessionRequest = it }
        writeRecordRequest?.also { this.writeRecordRequest = it }
    } ?: throw ResourceNotFoundException("Failed to update invoice [${invoiceParam.invoiceUuid()}]: No record existed in the database")

    fun findAllFromAddress(fromAddress: String): List<InvoiceDto> = InvoiceRecord
        .find { InvoiceTable.fromAddress eq fromAddress }
        .map { InvoiceDto.fromRecord(it) }

    fun findAllToAddresses(toAddresses: Collection<String>): List<InvoiceDto> = InvoiceRecord
        .find { InvoiceTable.toAddress inList toAddresses }
        .map { InvoiceDto.fromRecord(it) }

    fun findInvoiceUuidsWithFailedOracleApprovals(onlyIncludeUuids: Collection<UUID>? = null): List<UUID> = InvoiceTable
        .select {
            (InvoiceTable.status eq InvoiceStatus.APPROVAL_FAILURE.name)
                .let { queryClause ->
                    onlyIncludeUuids
                        ?.let { uuids -> queryClause.and(InvoiceTable.id inList uuids) }
                        ?: queryClause
                }
        }.map { it[InvoiceTable.id].value }

    fun findWritesOrNull(invoiceUuid: UUID): InvoiceWritesResponse? = InvoiceTable
        .slice(InvoiceTable.writeScopeRequest, InvoiceTable.writeSessionRequest, InvoiceTable.writeRecordRequest)
        .select { InvoiceTable.id eq invoiceUuid }
        .singleOrNull()
        ?.let { record ->
            InvoiceWritesResponse(
                writeScopeRequest = record[InvoiceTable.writeScopeRequest],
                writeSessionRequest = record[InvoiceTable.writeSessionRequest],
                writeRecordRequest = record[InvoiceTable.writeRecordRequest],
            )
        }

    fun findWriteScopeRequestOrNull(invoiceUuid: UUID): MsgWriteScopeRequest? = InvoiceTable
        .slice(InvoiceTable.writeScopeRequest)
        .select { InvoiceTable.id eq invoiceUuid }
        .singleOrNull()
        ?.get(InvoiceTable.writeScopeRequest)

    fun findWriteSessionRequestOrNull(invoiceUuid: UUID): MsgWriteSessionRequest? = InvoiceTable
        .slice(InvoiceTable.writeSessionRequest)
        .select { InvoiceTable.id eq invoiceUuid }
        .singleOrNull()
        ?.get(InvoiceTable.writeSessionRequest)

    fun findWriteRecordRequestOrNull(invoiceUuid: UUID): MsgWriteRecordRequest? = InvoiceTable
        .slice(InvoiceTable.writeRecordRequest)
        .select { InvoiceTable.id eq invoiceUuid }
        .singleOrNull()
        ?.get(InvoiceTable.writeRecordRequest)
}

class InvoiceRecord(uuid: EntityID<UUID>) : UUIDEntity(uuid) {
    companion object : InvoiceEntityClass(InvoiceTable)

    var data: Invoice by InvoiceTable.data
    var fromAddress: String by InvoiceTable.fromAddress
    var toAddress: String by InvoiceTable.toAddress
    var status: String by InvoiceTable.status
    var writeScopeRequest: MsgWriteScopeRequest by InvoiceTable.writeScopeRequest
    var writeSessionRequest: MsgWriteSessionRequest by InvoiceTable.writeSessionRequest
    var writeRecordRequest: MsgWriteRecordRequest by InvoiceTable.writeRecordRequest
    var createdTime: OffsetDateTime by InvoiceTable.createdTime
    var updatedTime: OffsetDateTime? by InvoiceTable.updatedTime

    val invoice: Invoice by lazy { data }
    val invoiceUuid: UUID by lazy { invoice.invoiceUuid.toUuidI() }
    val processingStatus: InvoiceStatus by lazy { InvoiceStatus.valueOf(status) }

    fun toDto(): InvoiceDto = InvoiceDto.fromRecord(this)
}

sealed interface InvoiceUpdateQueryParam {
    fun invoiceUuid(): UUID

    class InvoiceUuid(val uuid: UUID): InvoiceUpdateQueryParam {
        override fun invoiceUuid(): UUID = uuid
    }
    class InvoiceProto(val proto: Invoice): InvoiceUpdateQueryParam {
        private val uuid: UUID by lazy { proto.invoiceUuid.toUuidI() }

        override fun invoiceUuid(): UUID = uuid
    }
}

data class InvoiceWritesResponse(
    val writeScopeRequest: MsgWriteScopeRequest,
    val writeSessionRequest: MsgWriteSessionRequest,
    val writeRecordRequest: MsgWriteRecordRequest,
)
