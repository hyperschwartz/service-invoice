package tech.figure.invoice.domain.entities

import tech.figure.invoice.InvoiceProtos.Invoice
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.select
import tech.figure.invoice.util.enums.InvoiceProcessingStatus
import tech.figure.invoice.util.exposed.offsetDatetime
import tech.figure.invoice.util.exposed.proto
import tech.figure.invoice.util.extension.toUuid
import java.time.OffsetDateTime
import java.util.UUID

object InvoiceTable : UUIDTable(columnName = "invoice_uuid", name = "invoice") {
    val data = proto(name = "data", Invoice.getDefaultInstance())
    val fromAddress = text(name = "from_address")
    val toAddress = text(name = "to_address")
    val status = text(name = "status")
    val createdTime = offsetDatetime(name = "created_time")
    val updatedTime = offsetDatetime(name = "updated_time").nullable()
}

open class InvoiceEntityClass(invoiceTable: InvoiceTable): UUIDEntityClass<InvoiceRecord>(invoiceTable) {
    private fun insert(
        invoice: Invoice,
        processingStatus: InvoiceProcessingStatus,
        created: OffsetDateTime = OffsetDateTime.now()
    ): InvoiceRecord = new(invoice.invoiceUuid.toUuid()) {
        data = invoice
        fromAddress = invoice.fromAddress
        toAddress = invoice.toAddress
        status = processingStatus.name
        createdTime = created
    }

    fun upsert(
        invoice: Invoice,
        processingStatus: InvoiceProcessingStatus,
        upsertTime: OffsetDateTime = OffsetDateTime.now(),
    ): InvoiceRecord = findById(invoice.invoiceUuid.toUuid())?.apply {
        data = invoice
        fromAddress = invoice.fromAddress
        toAddress = invoice.toAddress
        status = processingStatus.name
        updatedTime = upsertTime
    } ?: insert(invoice = invoice, processingStatus = processingStatus, created = upsertTime)

    fun findAllFromAddress(fromAddress: String): List<Invoice> = InvoiceTable
        .select { InvoiceTable.fromAddress eq fromAddress }
        .map { it[InvoiceTable.data] }

    fun findAllToAddress(toAddress: String): List<Invoice> = InvoiceTable
        .select { InvoiceTable.toAddress eq toAddress }
        .map { it[InvoiceTable.data] }
}

class InvoiceRecord(uuid: EntityID<UUID>) : UUIDEntity(uuid) {
    companion object : InvoiceEntityClass(InvoiceTable)

    var data: Invoice by InvoiceTable.data
    var fromAddress: String by InvoiceTable.fromAddress
    var toAddress: String by InvoiceTable.toAddress
    var status: String by InvoiceTable.status
    var createdTime: OffsetDateTime by InvoiceTable.createdTime
    var updatedTime: OffsetDateTime? by InvoiceTable.updatedTime

    val invoice: Invoice by lazy { data }
    val invoiceUuid: UUID by lazy { invoice.invoiceUuid.toUuid() }
    val processingStatus: InvoiceProcessingStatus by lazy { InvoiceProcessingStatus.valueOf(status) }
}
