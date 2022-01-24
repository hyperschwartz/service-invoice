package io.provenance.invoice.web.controllers

import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.config.web.AppHeaders
import io.provenance.invoice.config.web.AppRoutes
import io.provenance.invoice.domain.wallet.WalletDetails
import io.provenance.invoice.repository.InvoiceRepository
import io.provenance.invoice.services.InvoiceService
import io.provenance.invoice.services.OnboardInvoiceRequest
import io.provenance.invoice.util.enums.InvoiceProcessingStatus
import mu.KLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("${AppRoutes.V1}/invoices", produces = ["application/json"])
class InvoiceControllerV1(
    private val invoiceRepository: InvoiceRepository,
    private val invoiceService: InvoiceService,
) {
    private companion object : KLogging()

    @GetMapping("/{uuid}")
    fun getByUuid(@PathVariable uuid: UUID): Invoice? = invoiceRepository.findByUuidOrNull(uuid)

    @PostMapping("/onboard")
    fun onboardInvoice(
        @RequestBody invoice: Invoice,
        @RequestHeader(AppHeaders.WALLET_ADDRESS) address: String,
        @RequestHeader(AppHeaders.WALLET_PUBLIC_KEY) publicKey: String,
    ): Invoice = invoiceService.onboardInvoice(request = OnboardInvoiceRequest(
        invoice = invoice,
        walletDetails = WalletDetails(address = address, publicKey = publicKey),
    ))
}

