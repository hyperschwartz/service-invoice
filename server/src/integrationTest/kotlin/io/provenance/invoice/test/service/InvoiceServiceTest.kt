package io.provenance.invoice.test.service

import helper.MockProtoUtil
import helper.TestConstants
import io.provenance.invoice.domain.wallet.WalletDetails
import io.provenance.invoice.services.InvoiceService
import io.provenance.invoice.services.OnboardInvoiceRequest
import io.provenance.invoice.testhelpers.IntTestBase
import io.provenance.invoice.util.extension.toProtoAny
import io.provenance.invoice.util.extension.toUuid
import io.provenance.invoice.util.extension.typedUnpack
import io.provenance.metadata.v1.MsgWriteRecordRequest
import io.provenance.metadata.v1.MsgWriteScopeRequest
import io.provenance.metadata.v1.MsgWriteSessionRequest
import io.provenance.scope.util.MetadataAddress
import io.provenance.scope.util.toUuid
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class InvoiceServiceTest : IntTestBase() {
    @Autowired lateinit var invoiceService: InvoiceService

    @Test
    fun testInvoiceOnboard() {
        val invoice = MockProtoUtil.getMockInvoice()
        val response = invoiceService.onboardInvoice(
            request = OnboardInvoiceRequest(
                invoice = invoice,
                walletDetails = WalletDetails(
                    address = TestConstants.VALID_ADDRESS,
                    publicKey = TestConstants.VALID_PUBLIC_KEY,
                )
            )
        )
        assertEquals(
            expected = invoice,
            actual = response.invoice,
            message = "Expected the output invoice to be identical to the input invoice",
        )
        assertEquals(
            expected = invoice.invoiceUuid.toUuid(),
            actual = response.payablesContractExecutionDetail.payableUuid,
            message = "The invoice's uuid should be sent in the response as the payable uuid",
        )
        assertEquals(
            expected = response.scopeGenerationDetail.writeScopeRequest.typedUnpack<MsgWriteScopeRequest>().scopeUuid.let { scopeUuid ->
                MetadataAddress.forScope(scopeUuid.toUuid()).toString()
            },
            actual = response.payablesContractExecutionDetail.scopeId,
            message = "Expected the scope id to be derived from the write scope request",
        )
        assertEquals(
            expected = "nhash",
            actual = response.payablesContractExecutionDetail.invoiceDenom,
            message = "Expected the invoice denomination to be in nhash. All invoices are created as nhash for now",
        )
        assertEquals(
            expected = "10".toBigDecimal(),
            actual = response.payablesContractExecutionDetail.invoiceTotal,
            message = "Expected the invoice total to be the default value from the MockProtoUtil",
        )
        assertEquals(
            expected = MsgWriteScopeRequest.getDefaultInstance().toProtoAny().typeUrl,
            actual = response.scopeGenerationDetail.writeScopeRequest.typeUrl,
            message = "Expected the write scope request to be the correct type",
        )
        assertEquals(
            expected = MsgWriteSessionRequest.getDefaultInstance().toProtoAny().typeUrl,
            actual = response.scopeGenerationDetail.writeSessionRequest.typeUrl,
            message = "Expected the write session request to be the correct type",
        )
        assertEquals(
            expected = MsgWriteRecordRequest.getDefaultInstance().toProtoAny().typeUrl,
            actual = response.scopeGenerationDetail.writeRecordRequest.typeUrl,
            message = "Expected the write record request to be the correct type",
        )
    }
}
