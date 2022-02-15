package io.provenance.invoice.web.controllers

import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.InvoiceProtos.LineItem
import io.provenance.invoice.config.web.AppRoutes
import io.provenance.invoice.util.mock.MockInvoice
import io.provenance.invoice.util.mock.MockInvoiceParams
import io.provenance.invoice.util.mock.MockLineItem
import io.provenance.invoice.util.mock.MockLineItemParams
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${AppRoutes.V1}/mockup", produces = ["application/json"])
class MockupControllerV1 {
    @PostMapping("/invoice")
    fun mockupInvoiceProto(@RequestBody params: MockInvoiceParams): Invoice = MockInvoice.fromParams(params).toProto()

    @PostMapping("/line-item")
    fun mockupInvoiceLineItem(@RequestBody params: MockLineItemParams): LineItem = MockLineItem.fromParams(params).toProto()
}
