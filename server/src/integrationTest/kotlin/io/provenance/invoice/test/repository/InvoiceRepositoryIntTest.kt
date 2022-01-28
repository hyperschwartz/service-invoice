package io.provenance.invoice.test.repository

import helper.MockProtoUtil
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import io.provenance.invoice.repository.InvoiceRepository
import io.provenance.invoice.testhelpers.IntTestBase
import io.provenance.invoice.util.enums.InvoiceProcessingStatus
import io.provenance.invoice.util.extension.toUuid
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class InvoiceRepositoryIntTest : IntTestBase() {
    @Autowired lateinit var invoiceRepository: InvoiceRepository

    @Test
    fun testInvoicePersistAndReload() {
        val invoice = MockProtoUtil.getMockInvoice()
        val upsertedInvoice = invoiceRepository.upsert(invoice, InvoiceProcessingStatus.PENDING_STAMP)
        assertEquals(
            expected = invoice,
            actual = upsertedInvoice,
            message = "The responding value from an upsert should be the unmodified input",
        )
        val foundInvoice = invoiceRepository.findByUuidOrNull(invoice.invoiceUuid.toUuid())
        assertEquals(
            expected = invoice,
            actual = foundInvoice,
            message = "Expected the invoice to be located by its uuid in the database",
        )
        val foundInvoiceExceptional = invoiceRepository.findByUuid(invoice.invoiceUuid.toUuid())
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
        invoiceRepository.upsert(firstInvoice, InvoiceProcessingStatus.PENDING_STAMP)
        invoiceRepository.upsert(secondInvoice, InvoiceProcessingStatus.PENDING_STAMP)
        val foundBySender = invoiceRepository.findAllByFromAddress(fromAddress).map { it.invoiceUuid.toUuid() }
        assertEquals(
            expected = 2,
            actual = foundBySender.size,
            message = "Both invoices should be found by the sender address",
        )
        assertTrue(
            actual = firstInvoice.invoiceUuid.toUuid() in foundBySender,
            message = "The first invoice should be present in the response list for the sender",
        )
        assertTrue(
            actual = secondInvoice.invoiceUuid.toUuid() in foundBySender,
            message = "The second invoice should be present in the response list for the sender",
        )
        val foundByReceiver = invoiceRepository.findAllByToAddress(toAddress).map { it.invoiceUuid.toUuid() }
        assertEquals(
            expected = 2,
            actual = foundByReceiver.size,
            message = "Both invoices should be found by the receiver address",
        )
        assertTrue(
            actual = firstInvoice.invoiceUuid.toUuid() in foundByReceiver,
            message = "The first invoice should be present in the response list for the receiver",
        )
        assertTrue(
            actual = secondInvoice.invoiceUuid.toUuid() in foundByReceiver,
            message = "The second invoice should be present in the response list for the receiver",
        )
    }
}
