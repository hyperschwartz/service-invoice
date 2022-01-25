package tech.figure.invoice.test.service

import helper.MockProtoUtil
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.figure.invoice.domain.wallet.WalletDetails
import tech.figure.invoice.services.InvoiceService
import tech.figure.invoice.services.OnboardInvoiceRequest
import tech.figure.invoice.testhelpers.IntTestBase
import tech.figure.invoice.testhelpers.testcontainers.IntTestConstants

class InvoiceServiceTest : IntTestBase() {
    @Autowired lateinit var invoiceService: InvoiceService

    @Test
    fun testInvoiceOnboard() {
        val invoice = invoiceService.onboardInvoice(
            request = OnboardInvoiceRequest(
                invoice = MockProtoUtil.getMockInvoice(),
                walletDetails = WalletDetails(
                    address = IntTestConstants.VALID_ADDRESS,
                    publicKey = IntTestConstants.VALID_PUBLIC_KEY,
                )
            )
        )
        // TODO: Use this working example as a way to flesh out the remaining database requirements
    }
}
