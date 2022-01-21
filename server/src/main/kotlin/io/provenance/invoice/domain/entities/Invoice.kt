package io.provenance.invoice.domain.entities

import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.domain.exceptions.ResourceNotFoundException
import io.provenance.invoice.util.enums.InvoiceProcessingStatus
import io.provenance.invoice.util.exposed.offsetDatetime
import io.provenance.invoice.util.exposed.proto
import io.provenance.invoice.util.extension.toUuid
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.time.OffsetDateTime
import java.util.UUID

object InvoiceTable : UUIDTable(columnName = "invoice_uuid", name = "invoice") {
    val invoiceUuid = uuid("invoice_uuid")
    val data = proto(name = "data", Invoice.getDefaultInstance())
    val status = text(name = "status")
    val createdTime = offsetDatetime(name = "created_time")
    val updatedTime = offsetDatetime(name = "updated_time").nullable()
}

open class InvoiceEntityClass(invoiceTable: InvoiceTable): UUIDEntityClass<InvoiceRecord>(invoiceTable) {
    private fun insert(
        invoice: Invoice,
        processingStatus: InvoiceProcessingStatus = InvoiceProcessingStatus.PENDING_STAMP,
        created: OffsetDateTime = OffsetDateTime.now()
    ): InvoiceRecord = new(invoice.invoiceUuid.toUuid()) {
        data = invoice
        status = processingStatus.name
        createdTime = created
    }

    fun upsert(
        invoice: Invoice,
        processingStatus: InvoiceProcessingStatus = InvoiceProcessingStatus.PENDING_STAMP,
        upsertTime: OffsetDateTime = OffsetDateTime.now(),
    ): InvoiceRecord = findRecordByUuidOrNull(invoice.invoiceUuid.toUuid())?.apply {
        data = invoice
        status = processingStatus.name
        updatedTime = upsertTime
    } ?: insert(invoice = invoice, processingStatus = processingStatus, created = upsertTime)

    fun findRecordByUuidOrNull(uuid: UUID): InvoiceRecord? = find { InvoiceTable.invoiceUuid eq uuid }.firstOrNull()

    fun findRecordByUuid(uuid: UUID): InvoiceRecord = findRecordByUuidOrNull(uuid)
        ?: throw ResourceNotFoundException("Failed to find invoice by uuid [$uuid]")

    fun findInvoiceByUuidOrNull(uuid: UUID): Invoice? = findRecordByUuidOrNull(uuid)?.invoice

    fun findInvoiceByUuid(uuid: UUID): Invoice = findRecordByUuid(uuid).invoice
}

class InvoiceRecord(uuid: EntityID<UUID>) : UUIDEntity(uuid) {
    companion object : InvoiceEntityClass(InvoiceTable)

    val invoiceUuid: UUID by InvoiceTable.invoiceUuid
    var data: Invoice by InvoiceTable.data
    var status: String by InvoiceTable.status
    var createdTime: OffsetDateTime by InvoiceTable.createdTime
    var updatedTime: OffsetDateTime? by InvoiceTable.updatedTime

    val processingStatus: InvoiceProcessingStatus by lazy { InvoiceProcessingStatus.valueOf(status) }
    val invoice: Invoice by lazy { data }
}
