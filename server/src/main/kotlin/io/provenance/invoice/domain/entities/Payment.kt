package io.provenance.invoice.domain.entities

import io.provenance.invoice.domain.dto.PaymentDto
import io.provenance.invoice.util.exposed.offsetDatetime
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.select
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

object PaymentTable : UUIDTable(columnName = "payment_uuid", name = "payment") {
    val paymentUuid = uuid(name = "payment_uuid")
    val invoiceUuid = uuid(name = "invoice_uuid").references(InvoiceTable.id)
    val paymentTime = offsetDatetime(name = "payment_time")
    val fromAddress = text(name = "from_address")
    val toAddress = text(name = "to_address")
    val paymentAmount = decimal(name = "payment_amount", precision = 1000, scale = 15)
    val createdTime = offsetDatetime(name = "created_time")
    val updatedTime = offsetDatetime(name = "updated_time").nullable()
}

open class PaymentEntityClass(paymentTable: PaymentTable): UUIDEntityClass<PaymentRecord>(paymentTable) {
    fun insert(
        invoiceUuid: UUID,
        paymentTime: OffsetDateTime,
        fromAddress: String,
        toAddress: String,
        paymentAmount: BigDecimal,
        paymentUuid: UUID = UUID.randomUUID(),
    ): PaymentRecord = findById(paymentUuid)
        ?.also { throw IllegalStateException("Payment [$paymentUuid] already exists in the database") }
        .run {
            new(paymentUuid) {
                this.invoiceUuid = invoiceUuid
                this.paymentTime = paymentTime
                this.fromAddress = fromAddress
                this.toAddress = toAddress
                this.paymentAmount = paymentAmount
                this.createdTime = OffsetDateTime.now()
            }
        }

    fun findAllByInvoiceUuid(invoiceUuid: UUID): List<PaymentDto> = PaymentTable
        .select { PaymentTable.invoiceUuid eq invoiceUuid }
        .map { PaymentDto.fromResultRow(it) }
}

class PaymentRecord(uuid: EntityID<UUID>) : UUIDEntity(uuid) {
    companion object : PaymentEntityClass(PaymentTable)

    val paymentUuid: UUID by PaymentTable.paymentUuid
    var invoiceUuid: UUID by PaymentTable.invoiceUuid
    var paymentTime: OffsetDateTime by PaymentTable.paymentTime
    var fromAddress: String by PaymentTable.fromAddress
    var toAddress: String by PaymentTable.toAddress
    var paymentAmount: BigDecimal by PaymentTable.paymentAmount
    var createdTime: OffsetDateTime by PaymentTable.createdTime
    var updatedTime: OffsetDateTime? by PaymentTable.updatedTime

    fun toDto(): PaymentDto = PaymentDto.fromRecord(this)
}
