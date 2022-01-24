package tech.figure.invoice.util.validation

import helper.MockProtoUtil
import helper.assertSucceeds
import org.junit.jupiter.api.Test
import tech.figure.invoice.util.enums.ExpectedDenom
import tech.figure.invoice.util.extension.toBigDecimalOrNull
import tech.figure.invoice.util.extension.toLocalDate
import tech.figure.invoice.util.extension.toProtoDate
import tech.figure.invoice.util.extension.toProtoDecimal
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFails

class InvoiceValidatorTest {
    @Test
    fun testDefaultInvoiceMockIsValid() {
        val invoice = MockProtoUtil.getMockInvoice()
        assertEquals(
            expected = 1,
            actual = invoice.lineItemsCount,
            message = "Sanity check: The mock util should generate a single line item by default",
        )
        assertSucceeds("The default mock invoice should pass validation") {
           InvoiceValidator.validateInvoice(invoice)
        }
    }

    @Test
    fun testDefaultLineItemMockIsValid() {
        val lineItem = MockProtoUtil.getMockLineItem()
        assertSucceeds("The default mock line item should pass validation") {
            InvoiceValidator.validateLineItem(invoiceUuid = UUID.randomUUID(), lineItem = lineItem)
        }
    }

    @Test
    fun testMissingInvoiceUuidFailsValidation() {
        val badInvoice = MockProtoUtil.getMockInvoice().toBuilder().clearInvoiceUuid().build()
        assertFails("An invoice missing its primary uuid should be rejected") {
            InvoiceValidator.validateInvoice(badInvoice)
        }
    }

    @Test
    fun testMissingInvoiceFromAddressValidation() {
        val badInvoice = MockProtoUtil.getMockInvoice().toBuilder().clearFromAddress().build()
        assertFails("An invoice missing its from address should be rejected") {
            InvoiceValidator.validateInvoice(badInvoice)
        }
    }

    @Test
    fun testMissingInvoiceToAddressValidation() {
        val badInvoice = MockProtoUtil.getMockInvoice().toBuilder().clearToAddress().build()
        assertFails("An invoice missing its to address should be rejected") {
            InvoiceValidator.validateInvoice(badInvoice)
        }
    }

    @Test
    fun testMissingInvoiceCreatedDateValidation() {
        val badInvoice = MockProtoUtil.getMockInvoice().toBuilder().clearInvoiceCreatedDate().build()
        assertFails("An invoice missing its created date should be rejected") {
            InvoiceValidator.validateInvoice(badInvoice)
        }
    }

    @Test
    fun testInvoiceCreatedDateAfterTodaysDateValidation() {
        val todaysDate = LocalDate.now()
        val badInvoice = MockProtoUtil.getMockInvoice().toBuilder().setInvoiceCreatedDate(todaysDate.plusDays(1).toProtoDate()).build()
        assertFails("An invoice with a created date in the future should be rejected") {
            InvoiceValidator.validateInvoice(badInvoice)
        }
    }

    @Test
    fun testInvoiceDueDateMissingValidation() {
        val badInvoice = MockProtoUtil.getMockInvoice().toBuilder().clearInvoiceDueDate().build()
        assertFails("An invoice missing its due date should be rejected") {
            InvoiceValidator.validateInvoice(badInvoice)
        }
    }

    @Test
    fun testInvoiceDueDateBeforeCreatedDateValidation() {
        val badInvoice = MockProtoUtil.getMockInvoice().toBuilder().also { invoiceBuilder ->
            invoiceBuilder.invoiceDueDate = invoiceBuilder.invoiceCreatedDate.toLocalDate().minusDays(1).toProtoDate()
        }.build()
        assertFails("An invoice with a due date before its created date should be rejected") {
            InvoiceValidator.validateInvoice(badInvoice)
        }
        assertSucceeds("An invoice with a due date and created date with on the same day should be accepted") {
            InvoiceValidator.validateInvoice(
                badInvoice.toBuilder().setInvoiceDueDate(badInvoice.invoiceCreatedDate).build()
            )
        }
    }

    @Test
    fun testInvoiceMissingDescriptionValidation() {
        val badInvoice = MockProtoUtil.getMockInvoice().toBuilder().clearDescription().build()
        assertFails("An invoice missing its description should be rejected") {
            InvoiceValidator.validateInvoice(badInvoice)
        }
    }

    @Test
    fun testInvoiceWithBadDenomValidation() {
        val badInvoice = MockProtoUtil.getMockInvoice().toBuilder().clearPaymentDenom().build()
        assertFails("An invoice missing its payment denomination should be rejected") {
            InvoiceValidator.validateInvoice(badInvoice)
        }
        assertFails("An invoice with an unrecognized denomination should be rejected") {
            InvoiceValidator.validateInvoice(badInvoice.toBuilder().setPaymentDenom("unreasonable denomination sorry").build())
        }
        ExpectedDenom.values().forEach { validDenom ->
            assertSucceeds("An invoice with a valid denomination of [${validDenom.expectedName}] should pass validation") {
                InvoiceValidator.validateInvoice(badInvoice.toBuilder().setPaymentDenom(validDenom.expectedName).build())
            }
        }
    }

    @Test
    fun testInvoiceWithNoLineItemsValidation() {
        val badInvoice = MockProtoUtil.getMockInvoice(lineItemAmount = 0)
        assertEquals(
            expected = 0,
            actual = badInvoice.lineItemsCount,
            message = "Sanity check: No line items should be rejected when the mock builder gets a request for zero items",
        )
        assertFails("An invoice with no line items should be rejected") {
            InvoiceValidator.validateInvoice(badInvoice)
        }
    }

    @Test
    fun testInvoiceWithBadTotalAmountsValidation() {
        val badInvoice = MockProtoUtil.getMockInvoice(lineItemAmount = 0).toBuilder().also { invoiceBuilder ->
            invoiceBuilder.addLineItems(
                MockProtoUtil.getMockLineItem(
                    quantity = 1,
                    price = "-10".toProtoDecimal(),
                )
            )
        }.build()
        assertFails("An invoice with a negative line item sum should be rejected") {
            InvoiceValidator.validateInvoice(badInvoice)
        }
        assertFails("An invoice with a zero sum on its line items should be rejected") {
            InvoiceValidator.validateInvoice(badInvoice.toBuilder().clearLineItems().also { invoiceBuilder ->
                invoiceBuilder.addLineItems(
                    MockProtoUtil.getMockLineItem(
                        quantity = 100,
                        price = BigDecimal.ZERO.toProtoDecimal(),
                    )
                )
            }.build())
        }
    }

    @Test
    fun testLineItemWithNoUuidValidation() {
        val badLineItem = MockProtoUtil.getMockLineItem().toBuilder().clearLineUuid().build()
        assertFails("A line item with a missing line uuid should be rejected") {
            InvoiceValidator.validateLineItem(invoiceUuid = UUID.randomUUID(), lineItem = badLineItem)
        }
    }

    @Test
    fun testLineItemWithNoNameValidation() {
        val badLineItem = MockProtoUtil.getMockLineItem().toBuilder().clearName().build()
        assertFails("A line item with a missing name should be rejected") {
            InvoiceValidator.validateLineItem(invoiceUuid = UUID.randomUUID(), lineItem = badLineItem)
        }
    }

    @Test
    fun testLineItemWithNoDescriptionValidation() {
        val badLineItem = MockProtoUtil.getMockLineItem().toBuilder().clearDescription().build()
        assertFails("A line item with a missing description should be rejected") {
            InvoiceValidator.validateLineItem(invoiceUuid = UUID.randomUUID(), lineItem = badLineItem)
        }
    }

    @Test
    fun testLineItemWithBadQuantityValidation() {
        val badLineItem = MockProtoUtil.getMockLineItem().toBuilder().setQuantity(0).build()
        assertFails("A line item with a quantity of zero should be rejected") {
            InvoiceValidator.validateLineItem(invoiceUuid = UUID.randomUUID(), lineItem = badLineItem)
        }
        assertFails("A line item with a quantity below zero should be rejected") {
            InvoiceValidator.validateLineItem(invoiceUuid = UUID.randomUUID(), lineItem = badLineItem.toBuilder().setQuantity(-1).build())
        }
    }

    @Test
    fun testLineItemWithMissingPriceValidation() {
        val badLineItem = MockProtoUtil.getMockLineItem().toBuilder().clearPrice().build()
        assertFails("A line item with a missing price should be rejected'") {
            InvoiceValidator.validateLineItem(invoiceUuid = UUID.randomUUID(), lineItem = badLineItem)
        }
    }

    @Test
    fun testLineItemWithBadPriceValidation() {
        val badLineItem = MockProtoUtil.getMockLineItem(price = BigDecimal.ZERO.toProtoDecimal())
        assertEquals(
            expected = BigDecimal.ZERO,
            actual = badLineItem.price.toBigDecimalOrNull(),
            message = "Sanity check: The mock builder should produce a price of zero when requested",
        )
        assertFails("A line item with a zero price should be rejected") {
            InvoiceValidator.validateLineItem(invoiceUuid = UUID.randomUUID(), lineItem = badLineItem)
        }
        assertFails("A line item with a negative price should be rejected") {
            InvoiceValidator.validateLineItem(invoiceUuid = UUID.randomUUID(), lineItem = badLineItem.toBuilder().setPrice("-0.01".toProtoDecimal()).build())
        }
        assertSucceeds("A line item with a very low amount should still be accepted") {
            InvoiceValidator.validateLineItem(invoiceUuid = UUID.randomUUID(), lineItem = badLineItem.toBuilder().setPrice("0.0000001".toProtoDecimal()).build())
        }
    }
}
