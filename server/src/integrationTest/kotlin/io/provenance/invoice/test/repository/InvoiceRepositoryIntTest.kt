package io.provenance.invoice.test.repository

import helper.MockProtoUtil
import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.domain.dto.InvoiceDto
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import io.provenance.invoice.repository.InvoiceRepository
import io.provenance.invoice.testhelpers.IntTestBase
import io.provenance.invoice.util.enums.InvoiceStatus
import io.provenance.invoice.util.extension.toUuidI
import io.provenance.invoice.util.extension.wrapListI
import io.provenance.metadata.v1.MsgWriteRecordRequest
import io.provenance.metadata.v1.MsgWriteScopeRequest
import io.provenance.metadata.v1.MsgWriteSessionRequest
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class InvoiceRepositoryIntTest : IntTestBase() {
    @Autowired lateinit var invoiceRepository: InvoiceRepository

    @Test
    fun testInvoiceInsertAndReload() {
        val invoice = MockProtoUtil.getMockInvoice()
        val insertedInvoice = insertInvoice(invoice)
        assertEquals(
            expected = invoice,
            actual = insertedInvoice.invoice,
            message = "The responding value from an upsert should be the unmodified input",
        )
        val foundInvoice = invoiceRepository.findByUuidOrNull(invoice.invoiceUuid.toUuidI())
        assertEquals(
            expected = invoice,
            actual = foundInvoice,
            message = "Expected the invoice to be located by its uuid in the database",
        )
        val foundInvoiceExceptional = invoiceRepository.findByUuid(invoice.invoiceUuid.toUuidI())
        assertEquals(
            expected = invoice,
            actual = foundInvoiceExceptional,
            message = "Expected the exceptional route to find an invoice or throw to succeed as well",
        )
    }

    @Test
    fun testFindByAddress() {
        val fromAddress = "${UUID.randomUUID()}-sender"
        val toAddress = "${UUID.randomUUID()}-receiver"
        val firstInvoice = MockProtoUtil.getMockInvoice().toBuilder().setFromAddress(fromAddress).setToAddress(toAddress).build()
        val secondInvoice = MockProtoUtil.getMockInvoice().toBuilder().setFromAddress(fromAddress).setToAddress(toAddress).build()
        assertNotEquals(
            illegal = firstInvoice.invoiceUuid.value,
            actual = secondInvoice.invoiceUuid.value,
            message = "Sanity check: Both generated invoices have different unique identifiers",
        )
        insertInvoice(firstInvoice)
        insertInvoice(secondInvoice)
        val foundBySender = invoiceRepository.findAllByFromAddress(fromAddress).map { it.invoiceUuid.toUuidI() }
        assertEquals(
            expected = 2,
            actual = foundBySender.size,
            message = "Both invoices should be found by the sender address",
        )
        assertTrue(
            actual = firstInvoice.invoiceUuid.toUuidI() in foundBySender,
            message = "The first invoice should be present in the response list for the sender",
        )
        assertTrue(
            actual = secondInvoice.invoiceUuid.toUuidI() in foundBySender,
            message = "The second invoice should be present in the response list for the sender",
        )
        val foundByReceiver = invoiceRepository.findAllByToAddress(toAddress).map { it.invoiceUuid.toUuidI() }
        assertEquals(
            expected = 2,
            actual = foundByReceiver.size,
            message = "Both invoices should be found by the receiver address",
        )
        assertTrue(
            actual = firstInvoice.invoiceUuid.toUuidI() in foundByReceiver,
            message = "The first invoice should be present in the response list for the receiver",
        )
        assertTrue(
            actual = secondInvoice.invoiceUuid.toUuidI() in foundByReceiver,
            message = "The second invoice should be present in the response list for the receiver",
        )
    }

    @Test
    fun testFailStateRetryQuery() {
        val invoiceDto = insertInvoice(MockProtoUtil.getMockInvoice())
        assertFalse(
            actual = invoiceDto.uuid in invoiceRepository.findInvoiceUuidsWithFailedOracleApprovals(),
            message = "The invoice dto should not show up in the failed oracle approval statuses when first boarded",
        )
        invoiceRepository.update(
            uuid = invoiceDto.uuid,
            status = InvoiceStatus.APPROVAL_FAILURE,
        )
        assertTrue(
            actual = invoiceDto.uuid in invoiceRepository.findInvoiceUuidsWithFailedOracleApprovals(),
            message = "The invoice dto should be included in the failed oracle approval statuses once its status is set to failed",
        )
        assertTrue(
            actual = invoiceDto.uuid in invoiceRepository.findInvoiceUuidsWithFailedOracleApprovals(invoiceDto.uuid.wrapListI()),
            message = "The invoice dto should be included in the failed oracle approval statuses if targeted directly",
        )
        assertFalse(
            actual = invoiceDto.uuid in invoiceRepository.findInvoiceUuidsWithFailedOracleApprovals(UUID.randomUUID().wrapListI()),
            message = "The invoice dto should not be included in the failed oracle approval statuses if a different uuid is targeted",
        )
    }

    @Test
    fun testFindAllByToAddresses() {
        val firstToAddress = "first-${UUID.randomUUID()}"
        val secondToAddress = "second-${UUID.randomUUID()}"
        val firstInvoice = insertInvoice(MockProtoUtil.getMockInvoice().toBuilder().setToAddress(firstToAddress).build())
        val secondInvoice = insertInvoice(MockProtoUtil.getMockInvoice().toBuilder().setToAddress(secondToAddress).build())
        val invoiceUuids = invoiceRepository.findAllByToAddresses(listOf(firstToAddress, secondToAddress)).map { it.invoiceUuid.toUuidI() }
        assertEquals(
            expected = 2,
            actual = invoiceUuids.size,
            message = "Expected only two invoices to be returned by the query",
        )
        assertTrue(
            actual = firstInvoice.uuid in invoiceUuids,
            message = "Expected the first invoice to be picked up by its toAddress",
        )
        assertTrue(
            actual = secondInvoice.uuid in invoiceUuids,
            message = "Expected the second invoice to be picked up by its toAddress",
        )
    }

    private fun insertInvoice(invoice: Invoice): InvoiceDto = invoiceRepository.insert(
        invoice = invoice,
        status = InvoiceStatus.PENDING_STAMP,
        writeScopeRequest = MsgWriteScopeRequest.getDefaultInstance(),
        writeSessionRequest = MsgWriteSessionRequest.getDefaultInstance(),
        writeRecordRequest = MsgWriteRecordRequest.getDefaultInstance(),
    )
}
