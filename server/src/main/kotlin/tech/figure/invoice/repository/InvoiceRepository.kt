package tech.figure.invoice.repository

import tech.figure.invoice.InvoiceProtos.Invoice
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Repository
import tech.figure.invoice.domain.entities.InvoiceRecord
import tech.figure.invoice.domain.exceptions.ResourceNotFoundException
import tech.figure.invoice.util.enums.InvoiceProcessingStatus
import java.util.UUID

@Repository
class InvoiceRepository {
    fun findByUuidOrNull(uuid: UUID): Invoice? = transaction { InvoiceRecord.findById(uuid)?.invoice }

    fun findByUuid(uuid: UUID): Invoice = findByUuidOrNull(uuid)
        ?: throw ResourceNotFoundException("Failed to find invoice by uuid [$uuid]")

    fun upsert(
        invoice: Invoice,
        status: InvoiceProcessingStatus
    ): Invoice = transaction { InvoiceRecord.upsert(invoice, status) }.invoice

    fun findAllByFromAddress(fromAddress: String): List<Invoice> =
        transaction { InvoiceRecord.findAllFromAddress(fromAddress) }

    fun findAllByToAddress(toAddress: String): List<Invoice> =
        transaction { InvoiceRecord.findAllToAddress(toAddress) }
}
