package io.provenance.invoice.web.controllers

import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.config.web.Routes
import io.provenance.invoice.repository.InvoiceRepository
import io.provenance.invoice.util.enums.InvoiceProcessingStatus
import mu.KLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("${Routes.V1}/invoices", produces = ["application/json"])
class InvoiceControllerV1(private val invoiceRepository: InvoiceRepository) {
    private companion object : KLogging()

    @GetMapping("/{uuid}")
    fun getByUuid(@PathVariable uuid: UUID): Invoice? = invoiceRepository.findByUuidOrNull(uuid)

    // TODO: Remove this route when the proper onboarding code is written
    @PostMapping("/testonly")
    fun upsertInvoice(
        @RequestBody invoice: Invoice,
        @RequestParam status: InvoiceProcessingStatus?,
    ): Invoice {
        return invoiceRepository.upsert(invoice, status ?: InvoiceProcessingStatus.PENDING_STAMP)
    }
}

