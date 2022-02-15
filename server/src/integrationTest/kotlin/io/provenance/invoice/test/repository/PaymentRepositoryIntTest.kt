package io.provenance.invoice.test.repository

import helper.assertEqualsBD
import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.domain.dto.InvoiceDto
import io.provenance.invoice.factory.InvoiceCalcFactory
import io.provenance.invoice.repository.InvoiceRepository
import io.provenance.invoice.repository.PaymentRepository
import io.provenance.invoice.testhelpers.IntTestBase
import io.provenance.invoice.util.enums.InvoiceStatus
import io.provenance.invoice.util.enums.PaymentStatus
import io.provenance.invoice.util.mock.MockInvoice
import io.provenance.metadata.v1.MsgWriteRecordRequest
import io.provenance.metadata.v1.MsgWriteScopeRequest
import io.provenance.metadata.v1.MsgWriteSessionRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaymentRepositoryIntTest : IntTestBase() {
    @Autowired lateinit var invoiceCalcFactory: InvoiceCalcFactory
    @Autowired lateinit var invoiceRepository: InvoiceRepository
    @Autowired lateinit var paymentRepository: PaymentRepository

    @Test
    fun testFindAllEmptyResult() {
        val payments = paymentRepository.findAllByInvoiceUuid(UUID.randomUUID())
        assertTrue(payments.isEmpty(), "A random uuid should not result in any payments being found")
    }

    @Test
    fun testInsertNewPayment() {
        val invoice = insertInvoice()
        val calc = invoiceCalcFactory.generate(invoice.uuid)
        assertEquals(
            expected = invoice.totalOwed,
            actual = calc.remainingOwed,
            message = "The entire sum should still be owed before a payment is made",
        )
        assertEquals(
            expected = InvoiceStatus.APPROVED,
            actual = calc.invoiceStatus,
            message = "The invoice should be in an approved status",
        )
        assertEquals(
            expected = PaymentStatus.REPAY_PERIOD,
            actual = calc.paymentStatus,
            message = "The calc should show it is ready for payment",
        )
        val payment = paymentRepository.insert(
            invoiceUuid = invoice.uuid,
            paymentTime = OffsetDateTime.now(),
            fromAddress = invoice.invoice.toAddress,
            toAddress = invoice.invoice.fromAddress,
            paymentAmount = invoice.totalOwed,
        )
        val payoffCalc = invoiceCalcFactory.generate(invoice.uuid)
        assertTrue(
            actual = payment.uuid in payoffCalc.payments.map { it.uuid },
            message = "The payment should show up in recorded payments on the calc",
        )
        assertEqualsBD(
            expected = BigDecimal.ZERO,
            actual = payoffCalc.remainingOwed,
            message = "The calc should now reflect that it was paid off",
        )
        assertEquals(
            expected = PaymentStatus.PAID_ON_TIME,
            actual = payoffCalc.paymentStatus,
            message = "The calc should show that it was paid on time because the payment was basically instant",
        )
    }

    private fun insertInvoice(
        invoice: Invoice = MockInvoice.defaultProto(),
        status: InvoiceStatus = InvoiceStatus.APPROVED,
    ): InvoiceDto = invoiceRepository.insert(
        invoice = invoice,
        status = status,
        writeScopeRequest = MsgWriteScopeRequest.getDefaultInstance(),
        writeSessionRequest = MsgWriteSessionRequest.getDefaultInstance(),
        writeRecordRequest = MsgWriteRecordRequest.getDefaultInstance(),
    )
}
