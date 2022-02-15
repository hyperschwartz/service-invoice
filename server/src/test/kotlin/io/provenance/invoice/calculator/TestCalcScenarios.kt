package io.provenance.invoice.calculator

import helper.assertEqualsBD
import helper.assertSingleI
import helper.assertZeroBD
import io.provenance.invoice.util.enums.InvoiceStatus
import io.provenance.invoice.util.enums.PaymentStatus
import io.provenance.invoice.util.extension.toBigDecimalI
import io.provenance.invoice.util.extension.toLocalDateI
import io.provenance.invoice.util.extension.toOffsetDateTimeI
import io.provenance.invoice.util.extension.toUuidI
import io.provenance.invoice.util.mock.MockInvoice
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestCalcScenarios {
    @Test
    fun testDefaultCalcGen() {
        val calcGen = MockInvoice.default().toCalcGen()
        val calcTime = calcGen.invoiceDto.created.plusMinutes(1)
        val calc = calcGen.genCalc(calcTime)
        assertEquals(
            expected = calcGen.invoiceDto.uuid,
            actual = calc.uuid,
            message = "The calc's uuid should always equate to the invoice uuid",
        )
        assertEquals(
            expected = calcTime,
            actual = calc.calcTime,
            message = "The calc's calc time should always equate to the input calc time",
        )
        assertEquals(
            expected = InvoiceStatus.APPROVED,
            actual = calc.invoiceStatus,
            message = "The invoice status of the default calculator should be approved",
        )
        assertEquals(
            expected = PaymentStatus.REPAY_PERIOD,
            actual = calc.paymentStatus,
            message = "The payment status for the calc should show the repay period when just created",
        )
        assertEquals(
            expected = calcGen.invoiceDto.invoice.fromAddress,
            actual = calc.ownerAddress,
            message = "The owner address on the calc should be the sender address from the invoice",
        )
        assertEquals(
            expected = calcGen.invoiceDto.invoice.toAddress,
            actual = calc.payerAddress,
            message = "The payer address on the calc should be the receiver address from the invoice",
        )
        assertEquals(
            expected = calcGen.invoiceDto.invoice.invoiceCreatedDate.toLocalDateI(),
            actual = calc.createdDate,
            message = "The created date for the calc should be the date the invoice was created",
        )
        assertEquals(
            expected = calcGen.invoiceDto.invoice.invoiceDueDate.toLocalDateI(),
            actual = calc.dueDate,
            message = "The due date for the calc should be the date the invoice was marked as due",
        )
        assertEquals(
            expected = calcGen.invoiceDto.invoice.description,
            actual = calc.description,
            message = "The description for the calc should be the description originally listed on the invoice",
        )
        assertTrue(
            actual = calc.payments.isEmpty(),
            message = "No payments should be present in the calc because none have been added",
        )
        assertZeroBD(
            actual = calc.paymentSum,
            message = "The payment sum should be zero because no payments have been added",
        )
        assertEquals(
            expected = "nhash",
            actual = calc.paymentDenom,
            message = "Expected the payment denom to be output on the root of the invoice calc",
        )
        assertEqualsBD(
            expected = calcGen.invoiceDto.totalOwed,
            actual = calc.originalOwed,
            message = "The calc original owed value should equal to the total owed on the invoice",
        )
        assertEqualsBD(
            expected = calcGen.invoiceDto.totalOwed,
            actual = calc.remainingOwed,
            message = "The calc remaining owed value should equal to the total owed on the invoice because no payments have been made",
        )
        assertEquals(
            expected = 0,
            actual = calc.paymentDelinquentDays,
            message = "The repay period is still active, so the payment is not yet delinquent and should show a value of zero",
        )
        assertNull(
            actual = calc.payoffTime,
            message = "The payoff time should not be set because no payments have been made",
        )
        val protoLineItem = calcGen.invoiceDto.invoice.lineItemsList.assertSingleI("A single line item should be added to the default invoice mock")
        val calcLineItem = calc.lineItems.assertSingleI("A single line item should be in the calc result")
        assertEquals(
            expected = protoLineItem.lineUuid.toUuidI(),
            actual = calcLineItem.uuid,
            message = "The calc line item should match its uuid with the proto",
        )
        assertEquals(
            expected = protoLineItem.name,
            actual = calcLineItem.name,
            message = "The calc line item should match its name with the proto",
        )
        assertEquals(
            expected = protoLineItem.description,
            actual = calcLineItem.description,
            message = "The calc line item should match its description with the proto",
        )
        assertEquals(
            expected = protoLineItem.quantity,
            actual = calcLineItem.quantity,
            message = "The calc line item should match its quantity with the proto",
        )
        assertEqualsBD(
            expected = protoLineItem.price.toBigDecimalI(),
            actual = calcLineItem.price,
            message = "The calc line item should match its price with the proto",
        )
        assertEquals(
            expected = protoLineItem.quantity.toBigDecimal() * protoLineItem.price.toBigDecimalI(),
            actual = calcLineItem.total,
            message = "The calc line item's total should be derived by multiplying the quantity and price of the proto",
        )
    }

    @Test
    fun testSimplePayment() {
        val createdTime = "2022-01-01T12:00Z".toOffsetDateTimeI()
        val totalOwed = "100".toBigDecimal()
        val paymentTime = createdTime.plusDays(3)
        val paymentAmount = "30".toBigDecimal()
        val calcGen = MockInvoice.builder()
            .createdDate(createdTime.toLocalDate())
            .addLineForAmount(totalOwed)
            .build()
            .toCalcGen()
            .addPayment(paymentAmount = paymentAmount, effectiveTime = paymentTime)
        val calcWithoutPayment = calcGen.genCalc(calcTime = paymentTime.minusNanos(1))
        assertEqualsBD(
            expected = totalOwed,
            actual = calcWithoutPayment.remainingOwed,
            message = "Before the payment is processed, the calc should show that the original total is still remaining",
        )
        assertTrue(
            actual = calcWithoutPayment.payments.isEmpty(),
            message = "The calc should not reflect the payment in its payments list before it should be processed",
        )
        assertZeroBD(
            actual = calcWithoutPayment.paymentSum,
            message = "The payment sum should be zero before the payment is processed",
        )
        assertEquals(
            expected = PaymentStatus.REPAY_PERIOD,
            actual = calcWithoutPayment.paymentStatus,
            message = "Should be in repay before payment",
        )
        val calcWithPayment = calcGen.genCalc(calcTime = paymentTime)
        assertEqualsBD(
            expected = totalOwed - paymentAmount,
            actual = calcWithPayment.remainingOwed,
            message = "After the payment is processed, the calc should show its value debited from the total",
        )
        assertEqualsBD(
            expected = paymentAmount,
            actual = calcWithPayment.paymentSum,
            message = "The payment sum should equate to the total of the single payment",
        )
        assertEquals(
            expected = PaymentStatus.REPAY_PERIOD,
            actual = calcWithoutPayment.paymentStatus,
            message = "Should be in repay after payment",
        )
        val calcGenPayment = calcGen.payments.assertSingleI("Only one payment should exist in the calc gen")
        val calcPayment = calcWithPayment.payments.assertSingleI("Only one payment record should exist in the calc")
        assertEquals(
            expected = calcGenPayment.uuid,
            actual = calcPayment.uuid,
            message = "The calc payment should have a uuid that matches that of the one in the calc gen",
        )
        assertEquals(
            expected = calcGen.invoiceDto.uuid,
            actual = calcPayment.invoiceUuid,
            message = "The invoice uuid in the calc gen payment should match the uuid of the original invoice",
        )
        assertEquals(
            expected = paymentTime,
            actual = calcPayment.effectiveTime,
            message = "The effective time of the calc payment should equate to the time at whcih the payment was added",
        )
        assertEqualsBD(
            expected = paymentAmount,
            actual = calcPayment.paymentAmount,
            message = "The payment amount in the calc should equate to the amount paid upfront",
        )
        assertEquals(
            expected = calcGen.invoiceDto.invoice.paymentDenom,
            actual = calcPayment.paymentDenom,
            message = "The payment denomination should equate to the invoice's denom",
        )
        assertEquals(
            expected = calcGen.invoiceDto.invoice.toAddress,
            actual = calcPayment.fromAddress,
            message = "All payments should come from the invoice receiver",
        )
        assertEqualsBD(
            expected = totalOwed,
            actual = calcPayment.owedBeforePayment,
            message = "The amount owed before payment should be the total owed because no payments have been made",
        )
        assertEqualsBD(
            expected = totalOwed - paymentAmount,
            actual = calcPayment.owedAfterPayment,
            message = "The amount owed after payment should be the result of subtracting the payment amount from the original total",
        )
    }

    @Test
    fun testMultiplePayments() {
        val createdTime = "2022-01-01T12:00Z".toOffsetDateTimeI()
        val totalOwed = "100".toBigDecimal()
        val firstPaymentTime = createdTime.plusDays(3)
        val firstPaymentAmount = "30".toBigDecimal()
        val secondPaymentTime = firstPaymentTime.plusDays(1)
        val secondPaymentAmount = "10".toBigDecimal()
        val calcGen = MockInvoice.builder()
            .createdDate(createdTime.toLocalDate())
            .addLineForAmount(totalOwed)
            .build()
            .toCalcGen()
            .addPayment(paymentAmount = firstPaymentAmount, effectiveTime = firstPaymentTime)
            .addPayment(paymentAmount = secondPaymentAmount, effectiveTime = secondPaymentTime)
        assertTrue(
            actual = calcGen.genCalc(firstPaymentTime.minusNanos(1)).payments.isEmpty(),
            message = "Before all payments' effective times, the calc should not have any payments",
        )
        assertEquals(
            expected = 1,
            actual = calcGen.genCalc(secondPaymentTime.minusNanos(1)).payments.size,
            message = "Before the second payment's effective time, the calc should only have the first payment",
        )
        val bothPaymentsCalc = calcGen.genCalc(secondPaymentTime)
        assertEquals(
            expected = 2,
            actual = bothPaymentsCalc.payments.size,
            message = "When calculating at the time of the second payment, both payments should be processed into the calc",
        )
        assertEquals(
            expected = totalOwed - firstPaymentAmount - secondPaymentAmount,
            actual = bothPaymentsCalc.remainingOwed,
            message = "The remaining owed amount should equate to both payment amounts subtracted from the original total",
        )
        assertEquals(
            expected = firstPaymentAmount + secondPaymentAmount,
            actual = bothPaymentsCalc.paymentSum,
            message = "The payment sum should be properly calculated",
        )
        val firstPayment = bothPaymentsCalc.payments.assertSingleI("The first payment should be in the calc") { it.effectiveTime == firstPaymentTime }
        assertEqualsBD(
            expected = totalOwed,
            actual = firstPayment.owedBeforePayment,
            message = "The amount owed before the first payment should be the invoice total",
        )
        assertEqualsBD(
            expected = totalOwed - firstPaymentAmount,
            actual = firstPayment.owedAfterPayment,
            message = "The amount owed after the first payment should be the payment amount subtracted from the invoice total",
        )
        val secondPayment = bothPaymentsCalc.payments.assertSingleI("The second payment should be in the calc") { it.effectiveTime == secondPaymentTime }
        assertEqualsBD(
            expected = firstPayment.owedAfterPayment,
            actual = secondPayment.owedBeforePayment,
            message = "The amount owed before the second payment should be the amount owed after the first payment applied",
        )
        assertEqualsBD(
            expected = totalOwed - firstPaymentAmount - secondPaymentAmount,
            actual = secondPayment.owedAfterPayment,
            message = "The amount owed after the second payment should be both payment amounts subtracted from the total owed",
        )
    }

    @Test
    fun testPayoffOnTime() {
        val createdTime = "2022-01-01T12:00Z".toOffsetDateTimeI()
        val totalOwed = "100".toBigDecimal()
        val paymentTime = createdTime.plusDays(15)
        val calcGen = MockInvoice.builder()
            .createdDate(createdTime.toLocalDate())
            .addLineForAmount(totalOwed)
            .build()
            .toCalcGen()
            .addPayment(paymentAmount = totalOwed, effectiveTime = paymentTime)
        val calc = calcGen.genCalc(paymentTime)
        assertZeroBD(
            actual = calc.remainingOwed,
            message = "The calc should show that no amount is owed",
        )
        assertEquals(
            expected = PaymentStatus.PAID_ON_TIME,
            actual = calc.paymentStatus,
            message = "The calc should show that the invoice was paid on time",
        )
        assertEquals(
            expected = paymentTime,
            actual = calc.payoffTime,
            message = "The payoff time should be set to the payment's effective time",
        )
        assertEquals(
            expected = 0,
            actual = calc.paymentDelinquentDays,
            message = "The amount of days delinquent should be set to zero for an on time payoff",
        )
    }

    @Test
    fun testPayoffLate() {
        val createdTime = "2022-01-14T12:00Z".toOffsetDateTimeI()
        val dueTime = createdTime.plusMonths(6)
        val totalOwed = "1053".toBigDecimal()
        val paymentTime = dueTime.plusDays(3)
        val calcGen = MockInvoice.builder()
            .createdDate(createdTime.toLocalDate())
            .dueDate(dueTime.toLocalDate())
            .addLineForAmount(totalOwed)
            .build()
            .toCalcGen()
            .addPayment(paymentAmount = totalOwed, effectiveTime = paymentTime)
        val calc = calcGen.genCalc(paymentTime)
        assertZeroBD(
            actual = calc.remainingOwed,
            message = "The calc should show that no amount is owed",
        )
        assertEquals(
            expected = PaymentStatus.PAID_LATE,
            actual = calc.paymentStatus,
            message = "The calc should show that the invoice was paid late",
        )
        assertEquals(
            expected = paymentTime,
            actual = calc.payoffTime,
            message = "The payoff time should be set to the payment's effective time",
        )
        assertEquals(
            expected = 3,
            actual = calc.paymentDelinquentDays,
            message = "The amount of days delinquent should reflect that the payment was late by three days",
        )
        assertEquals(
            expected = 3,
            actual = calcGen.genCalc(paymentTime.plusDays(1)).paymentDelinquentDays,
            message = "The delinquent days should not increment if the calc is made later",
        )
        assertEquals(
            expected = 3,
            actual = calcGen.genCalc(paymentTime.plusMonths(1)).paymentDelinquentDays,
            message = "The delinquent days should not increment if the calc is made later",
        )
        assertEquals(
            expected = 3,
            actual = calcGen.genCalc(paymentTime.plusYears(100)).paymentDelinquentDays,
            message = "The delinquent days should not increment if the calc is made crazy later",
        )
    }

    @Test
    fun testDelinquentInvoice() {
        val createdTime = "2022-01-01T12:00Z".toOffsetDateTimeI()
        val dueTime = createdTime.plusMonths(6)
        val calcGen = MockInvoice.builder()
            .createdDate(createdTime.toLocalDate())
            .dueDate(dueTime.toLocalDate())
            .build()
            .toCalcGen()
        // Gen a calc at the last nano of the due date
        val dueDateCalc = calcGen.genCalc(dueTime.toLocalDate().plusDays(1).toOffsetDateTimeI().minusNanos(1))
        assertEquals(
            expected = 0,
            actual = dueDateCalc.paymentDelinquentDays,
            message = "Delinquency should not start incrementing until the day after the payment is due",
        )
        assertEquals(
            expected = PaymentStatus.REPAY_PERIOD,
            actual = dueDateCalc.paymentStatus,
            message = "The payment status should still show the repay period",
        )
        // Create a calc at 00:00:00 for the day after the due date
        val delinquentTime = dueTime.toLocalDate().plusDays(1).toOffsetDateTimeI()
        repeat(30) { delinquencyOffset ->
            val delinquentCalc = calcGen.genCalc(delinquentTime.plusDays(delinquencyOffset.toLong()))
            val prefix = "[${delinquencyOffset + 1} days after due date]"
            assertEquals(
                expected = delinquencyOffset + 1,
                actual = delinquentCalc.paymentDelinquentDays,
                message = "$prefix The calc should show delinquency of the amount of days since the due date has passed",
            )
            assertEquals(
                expected = PaymentStatus.DELINQUENT,
                actual = delinquentCalc.paymentStatus,
                message = "$prefix The calc should start showing a status of delinquency",
            )
        }
    }

    @Test
    fun testRestrictedPaymentStatus() {
        // All statuses except approved should derive a restricted payment status
        InvoiceStatus.values().toList().minus(InvoiceStatus.APPROVED).forEach { invoiceStatus ->
            val calc = MockInvoice.default().toCustomCalcGen(startingInvoiceStatus = invoiceStatus).genCalc()
            assertEquals(
                expected = PaymentStatus.RESTRICTED,
                actual = calc.paymentStatus,
                message = "[Invoice Status $invoiceStatus] A restricted invoice status should be derived from the calc before the invoice is approved by the oracle",
            )
        }
    }
}
