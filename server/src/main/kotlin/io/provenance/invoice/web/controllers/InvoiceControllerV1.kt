package io.provenance.invoice.web.controllers

import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.config.web.AppHeaders
import io.provenance.invoice.config.web.AppRoutes
import io.provenance.invoice.domain.wallet.WalletDetails
import io.provenance.scope.objectstore.util.base64EncodeString
import mu.KLogging
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.provenance.invoice.repository.InvoiceRepository
import io.provenance.invoice.services.InvoiceService
import io.provenance.invoice.services.OnboardInvoiceRequest
import io.provenance.invoice.services.OnboardInvoiceResponse
import java.util.UUID

@RestController
@RequestMapping("${AppRoutes.V1}/invoices", produces = ["application/json"])
@CrossOrigin(origins = ["http://localhost:3000"])
class InvoiceControllerV1(
    private val invoiceRepository: InvoiceRepository,
    private val invoiceService: InvoiceService,
) {
    private companion object : KLogging()

    @GetMapping("/{uuid}")
    fun getByUuid(@PathVariable uuid: UUID): String? = invoiceRepository.findByUuidOrNull(uuid)?.toByteArray()?.base64EncodeString()

    @PostMapping("/onboard")
    fun onboardInvoice(
        @RequestBody invoice: Invoice,
        @RequestHeader(AppHeaders.ADDRESS) address: String,
        @RequestHeader(AppHeaders.PUBLIC_KEY) publicKey: String,
    ): OnboardInvoiceResponse = invoiceService.onboardInvoice(
        request = OnboardInvoiceRequest(
            invoice = invoice,
            walletDetails = WalletDetails(address = address, publicKey = publicKey),
        )
    )

    @GetMapping("/address/from/{fromAddress}")
    fun getByFromAddress(@PathVariable fromAddress: String): List<ByteArray> = invoiceRepository.findAllByFromAddress(fromAddress).map { it.toByteArray() }

    @GetMapping("/address/to/{toAddress}")
    fun getByToAddress(@PathVariable toAddress: String): List<ByteArray> = invoiceRepository.findAllByToAddress(toAddress).map { it.toByteArray() }
}
