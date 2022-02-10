package io.provenance.invoice.util.validation

import helper.MockInvoiceUtil
import helper.assertSucceeds
import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.InvoiceProtos.LineItem
import io.provenance.invoice.util.enums.ExpectedDenom
import io.provenance.invoice.util.extension.toBigDecimalOrNullI
import io.provenance.invoice.util.extension.toLocalDateI
import io.provenance.invoice.util.extension.toProtoDateI
import io.provenance.invoice.util.extension.toProtoDecimalI
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InvoiceValidationTest {
    @Test
    fun testDefaultInvoiceMockIsValid() {
        val invoice = MockInvoiceUtil.getMockInvoice()
        assertEquals(
            expected = 1,
            actual = invoice.lineItemsCount,
            message = "Sanity check: The mock util should generate a single line item by default",
        )
        assertSucceeds("The default mock invoice should pass validation") {
            ValidatedInvoice.new(invoice).generateValidationReport().throwFailures()
        }
    }

    @Test
    fun testDefaultLineItemMockIsValid() {
        val lineItem = MockInvoiceUtil.getMockLineItem()
        assertSucceeds("The default mock line item should pass validation") {
            ValidatedLineItem.new(lineItem).generateValidationReport().throwFailures()
        }
    }

    @Test
    fun testMissingInvoiceUuidFailsValidation() {
        val badInvoice = MockInvoiceUtil.getMockInvoice().toBuilder().clearInvoiceUuid().build()
        val validated = ValidatedInvoice.new(badInvoice)
        assertFalse(
            actual = validated.uuid.isValid,
            message = "Expected the uuid value to be invalid",
        )
        val report = validated.generateValidationReport()
        assertTrue(
            actual = report.hadError<InvoiceValidationError.InvalidInvoiceUuid>(),
            message = "An invoice missing its primary uuid should be rejected",
        )
        assertFails("The failed report should throw an exception when requested") { report.throwFailures() }
    }

    @Test
    fun testMissingInvoiceFromAddressValidation() {
        val badInvoice = MockInvoiceUtil.getMockInvoice().toBuilder().clearFromAddress().build()
        val validated = ValidatedInvoice.new(badInvoice)
        assertFalse(
            actual = validated.fromAddress.isValid,
            message = "Expected the fromAddress value to be invalid",
        )
        val report = validated.generateValidationReport()
        assertTrue(
            actual = report.hadError<InvoiceValidationError.InvalidInvoiceFromAddress>(),
            message = "An invoice missing its from address should be rejected",
        )
        assertFails("The failed report should throw an exception when requested") { report.throwFailures() }
    }

    @Test
    fun testMissingInvoiceToAddressValidation() {
        val badInvoice = MockInvoiceUtil.getMockInvoice().toBuilder().clearToAddress().build()
        val validated = ValidatedInvoice.new(badInvoice)
        assertFalse(
            actual = validated.toAddress.isValid,
            message = "Expected the toAddress value to be invalid",
        )
        val report = validated.generateValidationReport()
        assertTrue(
            actual = report.hadError<InvoiceValidationError.InvalidInvoiceToAddress>(),
            message = "An invoice missing its to address should be rejected",
        )
        assertFails("The failed report should throw an exception when requested") { report.throwFailures() }
    }

    @Test
    fun testMissingInvoiceCreatedDateValidation() {
        val badInvoice = MockInvoiceUtil.getMockInvoice().toBuilder().clearInvoiceCreatedDate().build()
        val validated = ValidatedInvoice.new(badInvoice)
        assertFalse(
            actual = validated.createdDate.isValid,
            message = "Expected the created date value to be invalid",
        )
        val report = validated.generateValidationReport()
        assertTrue(
            actual = report.hadError<InvoiceValidationError.InvalidInvoiceCreatedDate>(),
            message = "An invoice missing its created date should be rejected",
        )
        assertFails("The failed report should throw an exception when requested") { report.throwFailures() }
    }

    @Test
    fun testInvoiceCreatedDateAfterTodaysDateValidation() {
        val todaysDate = LocalDate.now()
        val badInvoice = MockInvoiceUtil.getMockInvoice().toBuilder().setInvoiceCreatedDate(todaysDate.plusDays(1).toProtoDateI()).build()
        val validated = ValidatedInvoice.new(badInvoice)
        assertFalse(
            actual = validated.createdDate.isValid,
            message = "Expected the created date value to be invalid",
        )
        val report = validated.generateValidationReport()
        assertTrue(
            actual = report.hadError<InvoiceValidationError.FutureInvoiceCreatedDate>(),
            message = "An invoice with a created date in the future should be rejected",
        )
        assertFails("The failed report should throw an exception when requested") { report.throwFailures() }
    }

    @Test
    fun testInvoiceDueDateMissingValidation() {
        val badInvoice = MockInvoiceUtil.getMockInvoice().toBuilder().clearInvoiceDueDate().build()
        val validated = ValidatedInvoice.new(badInvoice)
        assertFalse(
            actual = validated.dueDate.isValid,
            message = "Expected the due date to be invalid",
        )
        val report = validated.generateValidationReport()
        assertTrue(
            actual = report.hadError<InvoiceValidationError.InvalidInvoiceDueDate>(),
            message = "An invoice missing its due date should be rejected",
        )
        assertFails("The failed report should throw an exception when requested") { report.throwFailures() }
    }

    @Test
    fun testInvoiceDueDateBeforeCreatedDateValidation() {
        val badInvoice = MockInvoiceUtil.getMockInvoice().toBuilder().also { invoiceBuilder ->
            invoiceBuilder.invoiceDueDate = invoiceBuilder.invoiceCreatedDate.toLocalDateI().minusDays(1).toProtoDateI()
        }.build()
        val validated = ValidatedInvoice.new(badInvoice)
        assertFalse(
            actual = validated.dueDate.isValid,
            message = "Expected the due date field to be invalid",
        )
        val report = validated.generateValidationReport()
        assertTrue(
            actual = report.hadError<InvoiceValidationError.InvoiceDueDateBeforeCreatedDate>(),
            message = "An invoice with a due date before its create date should be rejected",
        )
        assertFails("The failed report should throw an exception when requested") { report.throwFailures() }
        assertSucceeds("An invoice with a due date and created date on the same day should be accepted") {
            ValidatedInvoice.new(
                badInvoice.toBuilder().setInvoiceDueDate(badInvoice.invoiceCreatedDate).build()
            ).generateValidationReport().throwFailures()
        }
    }

    @Test
    fun testInvoiceMissingDescriptionValidation() {
        val badInvoice = MockInvoiceUtil.getMockInvoice().toBuilder().clearDescription().build()
        val validated = ValidatedInvoice.new(badInvoice)
        assertFalse(
            actual = validated.description.isValid,
            message = "Expected the description field to be invalid",
        )
        val report = validated.generateValidationReport()
        assertTrue(
            actual = report.hadError<InvoiceValidationError.InvalidInvoiceDescription>(),
            message = "An invoice missing its description should be rejected",
        )
        assertFails("The failed report should throw an exception when requested") { report.throwFailures() }
    }

    @Test
    fun testInvoiceWithBadDenomValidation() {
        fun testInvalidDenomInvoice(invoice: Invoice, description: String) {
            val validated = ValidatedInvoice.new(invoice)
            assertFalse(
                actual = validated.paymentDenom.isValid,
                message = "[$description]: Expected the paymentDenom field to be invalid",
            )
            val report = validated.generateValidationReport()
            assertTrue(
                actual = report.hadError<InvoiceValidationError.InvalidInvoiceDenom>(),
                message = "[$description]: The InvalidInvoiceDenom error should be added to the validation report",
            )
            assertFails("[$description]: The failed report should throw an exception when requested") { report.throwFailures() }
        }
        testInvalidDenomInvoice(
            invoice = MockInvoiceUtil.getMockInvoice().toBuilder().clearPaymentDenom().build(),
            description = "Missing payment denom",
        )
        testInvalidDenomInvoice(
            invoice = MockInvoiceUtil.getMockInvoice().toBuilder().setPaymentDenom("unreasonable denomination sorry").build(),
            description = "Unrecognized denom",
        )
        ExpectedDenom.values().forEach { validDenom ->
            assertSucceeds("An invoice with a valid denomination of [${validDenom.expectedName}] should pass validation") {
                ValidatedInvoice.new(MockInvoiceUtil.getMockInvoice().toBuilder().setPaymentDenom(validDenom.expectedName).build())
                    .generateValidationReport()
                    .throwFailures()
            }
        }
    }

    @Test
    fun testInvoiceWithNoLineItemsValidation() {
        val badInvoice = MockInvoiceUtil.getMockInvoice(lineItemAmount = 0)
        assertEquals(
            expected = 0,
            actual = badInvoice.lineItemsCount,
            message = "Sanity check: No line items should be rejected when the mock builder gets a request for zero items",
        )
        val validated = ValidatedInvoice.new(badInvoice)
        assertFalse(
            actual = validated.lineItemCheck.isValid,
            message = "The line item checker should be invalid",
        )
        val report = validated.generateValidationReport()
        assertTrue(
            actual = report.hadError<InvoiceValidationError.NoLineItems>(),
            message = "An invoice with no line items should result in the correct error",
        )
        assertFails("The failed report should throw an exception when requested") { report.throwFailures() }
    }

    @Test
    fun testInvoiceWithBadTotalAmountsValidation() {
        fun testInvalidLineSumInvoice(invoice: Invoice, description: String) {
            val validated = ValidatedInvoice.new(invoice)
            assertFalse(
                actual = validated.lineItemCheck.isValid,
                message = "[$description] The line item checker should be invalid",
            )
            val report = validated.generateValidationReport()
            assertTrue(
                actual = report.hadError<InvoiceValidationError.InvalidLineItemTotal>(),
                message = "[$description] The invalid line item total error should be added to the validation report",
            )
            assertFails("[$description] The failed report should throw an exception when requested") { report.throwFailures() }
        }
        testInvalidLineSumInvoice(
            invoice = MockInvoiceUtil.getMockInvoice(lineItemAmount = 0).toBuilder().also { invoiceBuilder ->
                invoiceBuilder.addLineItems(
                    MockInvoiceUtil.getMockLineItem(
                        quantity = 1,
                        price = "-10".toProtoDecimalI(),
                    )
                )
            }.build(),
            description = "Negative line item sum",
        )
        testInvalidLineSumInvoice(
            invoice = MockInvoiceUtil.getMockInvoice(lineItemAmount = 0).toBuilder().also { invoiceBuilder ->
                invoiceBuilder.addLineItems(
                    MockInvoiceUtil.getMockLineItem(
                        quantity = 100,
                        price = BigDecimal.ZERO.toProtoDecimalI(),
                    )
                )
            }.build(),
            description = "Zero line item sum",
        )
    }

    @Test
    fun testLineItemWithNoUuidValidation() {
        val badLineItem = MockInvoiceUtil.getMockLineItem().toBuilder().clearLineUuid().build()
        val validated = ValidatedLineItem.new(badLineItem)
        assertFalse(
            actual = validated.uuid.isValid,
            message = "The uuid field should be invalid",
        )
        val report = validated.generateValidationReport()
        assertTrue(
            actual = report.hadError<InvoiceValidationError.InvalidLineUuid>(),
            message = "The invalid line uuid error should be included in the report",
        )
        assertFails("The failed report should throw an exception when requested") { report.throwFailures() }
    }

    @Test
    fun testLineItemWithNoNameValidation() {
        val badLineItem = MockInvoiceUtil.getMockLineItem().toBuilder().clearName().build()
        val validated = ValidatedLineItem.new(badLineItem)
        assertFalse(
            actual = validated.name.isValid,
            message = "The name field should be invalid",
        )
        val report = validated.generateValidationReport()
        assertTrue(
            actual = report.hadError<InvoiceValidationError.InvalidLineName>(),
            message = "The invalid line name error should be included in the report",
        )
        assertFails("The failed report should throw an exception when requested") { report.throwFailures() }
    }

    @Test
    fun testLineItemWithNoDescriptionValidation() {
        val badLineItem = MockInvoiceUtil.getMockLineItem().toBuilder().clearDescription().build()
        val validated = ValidatedLineItem.new(badLineItem)
        assertFalse(
            actual = validated.description.isValid,
            message = "The description field should be invalid",
        )
        val report = validated.generateValidationReport()
        assertTrue(
            actual = report.hadError<InvoiceValidationError.InvalidLineDescription>(),
            message = "The invalid line description error should be included in the report",
        )
        assertFails("The failed report should throw an exception when requested") { report.throwFailures() }
    }

    @Test
    fun testLineItemWithBadQuantityValidation() {
        fun testInvalidQuantityLineItem(lineItem: LineItem, description: String) {
            val validated = ValidatedLineItem.new(lineItem)
            assertFalse(
                actual = validated.quantity.isValid,
                message = "[$description]: The quantity field should be invalid",
            )
            val report = validated.generateValidationReport()
            assertTrue(
                actual = report.hadError<InvoiceValidationError.InvalidLineQuantity>(),
                message = "[$description]: The invalid line quantity error should be included in the report",
            )
            assertFails("[$description]: The failed report should throw an exception when requested") { report.throwFailures() }
        }
        val badLineItem = MockInvoiceUtil.getMockLineItem().toBuilder().setQuantity(0).build()
        testInvalidQuantityLineItem(
            lineItem = badLineItem,
            description = "Zero quantity line item",
        )
        testInvalidQuantityLineItem(
            lineItem = badLineItem.toBuilder().setQuantity(-1).build(),
            description = "Negative quantity line item",
        )
    }

    @Test
    fun testLineItemWithMissingPriceValidation() {
        val badLineItem = MockInvoiceUtil.getMockLineItem().toBuilder().clearPrice().build()
        val validated = ValidatedLineItem.new(badLineItem)
        assertFalse(
            actual = validated.price.isValid,
            message = "The price field should be invalid",
        )
        val report = validated.generateValidationReport()
        assertTrue(
            actual = report.hadError<InvoiceValidationError.InvalidLinePrice>(),
            message = "The invalid line price error should be included in the report",
        )
        assertFails("The failed report should throw an exception when requested") { report.throwFailures() }
    }

    @Test
    fun testLineItemWithBadPriceValidation() {
        fun testInvalidPriceLineItem(lineItem: LineItem, description: String) {
            val validated = ValidatedLineItem.new(lineItem)
            assertFalse(
                actual = validated.price.isValid,
                message = "[$description]: The price field should be invalid",
            )
            val report = validated.generateValidationReport()
            assertTrue(
                actual = report.hadError<InvoiceValidationError.LinePriceTooLow>(),
                message = "[$description]: The line price too low error should be included in the report",
            )
            assertFails("[$description] The failed report should throw an exception when requested") { report.throwFailures() }
        }
        val badLineItem = MockInvoiceUtil.getMockLineItem(price = BigDecimal.ZERO.toProtoDecimalI())
        assertEquals(
            expected = BigDecimal.ZERO,
            actual = badLineItem.price.toBigDecimalOrNullI(),
            message = "Sanity check: The mock builder should produce a price of zero when requested",
        )
        testInvalidPriceLineItem(
            lineItem = badLineItem,
            description = "Zero price line item",
        )
        testInvalidPriceLineItem(
            lineItem = badLineItem.toBuilder().setPrice("-0.01".toProtoDecimalI()).build(),
            description = "Negative price line item",
        )
        assertSucceeds("A line item with a very low amount should still be accepted") {
            ValidatedLineItem.new(badLineItem.toBuilder().setPrice("0.0000001".toProtoDecimalI()).build())
                .generateValidationReport()
                .throwFailures()
        }
    }
}
