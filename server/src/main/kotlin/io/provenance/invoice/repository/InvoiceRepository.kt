package io.provenance.invoice.repository

import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.domain.entities.InvoiceRecord
import io.provenance.invoice.domain.exceptions.ResourceNotFoundException
import io.provenance.invoice.util.enums.InvoiceProcessingStatus
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Repository
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
}
