package tech.figure.invoice.web.controllers

import tech.figure.invoice.InvoiceProtos.Invoice
import mu.KLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tech.figure.invoice.config.web.AppHeaders
import tech.figure.invoice.config.web.AppRoutes
import tech.figure.invoice.domain.wallet.WalletDetails
import tech.figure.invoice.repository.InvoiceRepository
import tech.figure.invoice.services.InvoiceService
import tech.figure.invoice.services.OnboardInvoiceRequest
import tech.figure.invoice.services.OnboardInvoiceResponse
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
        @RequestHeader(AppHeaders.ADDRESS) address: String,
        @RequestHeader(AppHeaders.PUBLIC_KEY) publicKey: String,
    ): OnboardInvoiceResponse = invoiceService.onboardInvoice(
        request = OnboardInvoiceRequest(
            invoice = invoice,
            walletDetails = WalletDetails(address = address, publicKey = publicKey),
        )
    )

    @GetMapping("/address/from/{fromAddress}")
    fun getByFromAddress(@PathVariable fromAddress: String): List<Invoice> = invoiceRepository.findAllByFromAddress(fromAddress)

    @GetMapping("/address/to/{toAddress}")
    fun getByToAddress(@PathVariable toAddress: String): List<Invoice> = invoiceRepository.findAllByToAddress(toAddress)
}

