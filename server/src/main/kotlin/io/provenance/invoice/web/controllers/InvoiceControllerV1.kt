package io.provenance.invoice.web.controllers

import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.calculator.InvoiceCalc
import io.provenance.invoice.config.web.AppHeaders
import io.provenance.invoice.config.web.AppRoutes
import io.provenance.invoice.domain.dto.InvoiceDto
import io.provenance.invoice.domain.entities.InvoiceWritesResponse
import io.provenance.invoice.factory.InvoiceCalcFactory
import io.provenance.scope.objectstore.util.base64EncodeString
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
import io.provenance.metadata.v1.MsgWriteRecordRequest
import io.provenance.metadata.v1.MsgWriteScopeRequest
import io.provenance.metadata.v1.MsgWriteSessionRequest
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.RequestParam
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("${AppRoutes.V1}/invoices", produces = ["application/json"])
class InvoiceControllerV1(
    private val invoiceCalcFactory: InvoiceCalcFactory,
    private val invoiceRepository: InvoiceRepository,
    private val invoiceService: InvoiceService,
) {
    @GetMapping("/{uuid}")
    fun getByUuid(@PathVariable uuid: UUID): String? = invoiceRepository.findByUuidOrNull(uuid)?.toByteArray()?.base64EncodeString()

    @PostMapping("/onboard")
    fun onboardInvoice(
        @RequestBody invoice: Invoice,
        @RequestHeader(AppHeaders.ADDRESS) address: String,
    ): OnboardInvoiceResponse = invoiceService.onboardInvoice(
        request = OnboardInvoiceRequest(
            invoice = invoice,
            walletAddress = address,
        )
    )

    @GetMapping("/address/from/{fromAddress}")
    fun getByFromAddress(@PathVariable fromAddress: String): List<InvoiceDto> = invoiceCalcFactory.generateMany(invoiceRepository.findAllByFromAddress(fromAddress))

    @GetMapping("/address/to/{toAddress}")
    fun getByToAddress(@PathVariable toAddress: String): List<InvoiceDto> = invoiceCalcFactory.generateMany(invoiceRepository.findAllByToAddress(toAddress))

    @PostMapping("/address/all")
    fun getByToAddresses(@RequestBody request: ToAddressRequest): List<InvoiceDto> = invoiceCalcFactory.generateMany(invoiceRepository.findAllByToAddresses(request.addresses))

    @GetMapping("/calc/{uuid}")
    fun getCalc(
        @PathVariable uuid: UUID,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @RequestParam calcTime: OffsetDateTime?,
    ): InvoiceCalc = invoiceCalcFactory.generate(invoiceUuid = uuid, calcTime = calcTime ?: OffsetDateTime.now())

    @GetMapping("/msg/all/{uuid}")
    fun getWritesForInvoice(@PathVariable uuid: UUID): InvoiceWritesResponse? = invoiceRepository.findWritesOrNull(uuid)

    @GetMapping("/msg/writeScopeRequest/{uuid}")
    fun getWriteScopeRequestForInvoice(@PathVariable uuid: UUID): MsgWriteScopeRequest? =
        invoiceRepository.findWriteScopeRequestOrNull(uuid)

    @GetMapping("/msg/writeSessionRequest/{uuid}")
    fun getWriteSessionRequestForInvoice(@PathVariable uuid: UUID): MsgWriteSessionRequest? =
        invoiceRepository.findWriteSessionRequestOrNull(uuid)

    @GetMapping("/msg/writeRecordRequest/{uuid}")
    fun getWriteRecordRequestForInvoice(@PathVariable uuid: UUID): MsgWriteRecordRequest? =
        invoiceRepository.findWriteRecordRequestOrNull(uuid)
}

data class ToAddressRequest(val addresses: List<String>)
