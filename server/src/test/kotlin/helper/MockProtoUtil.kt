package helper

import tech.figure.invoice.InvoiceProtos.Invoice
import tech.figure.invoice.InvoiceProtos.LineItem
import tech.figure.invoice.UtilProtos
import tech.figure.invoice.InvoiceProtosBuilders
import tech.figure.invoice.util.enums.ExpectedDenom
import tech.figure.invoice.util.extension.toProtoDate
import tech.figure.invoice.util.extension.toProtoDecimal
import tech.figure.invoice.util.randomProtoUuid
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertTrue

object MockProtoUtil {
    fun getMockInvoice(lineItemAmount: Int = 1): Invoice = InvoiceProtosBuilders.Invoice {
        // Ensure this value isn't being abused
        assertTrue(lineItemAmount >= 0, "Line item amount [$lineItemAmount] should be at least zero")
        invoiceUuid = randomProtoUuid()
        fromAddress = "sender"
        toAddress = "receiver"
        invoiceCreatedDate = LocalDate.now().toProtoDate()
        invoiceDueDate = LocalDate.now().plusMonths(1).toProtoDate()
        description = "Test invoice ${UUID.randomUUID()}"
        paymentDenom = ExpectedDenom.NHASH.expectedName
        for (i in 0 until lineItemAmount) {
            val iteration = i.plus(1).toBigDecimal()
            addLineItems(getMockLineItem(
                description = "Generated item [$iteration]",
                quantity = iteration.toInt(),
                price = "10".toBigDecimal().times(iteration).toProtoDecimal(),
            ))
        }
    }

    fun getMockLineItem(
        lineUuid: UtilProtos.UUID = randomProtoUuid(),
        name: String = "money request",
        description: String = "GIMME THAT MONEY",
        quantity: Int = 1,
        price: UtilProtos.Decimal = "100".toBigDecimal().toProtoDecimal(),
    ): LineItem = InvoiceProtosBuilders.LineItem {
        this.lineUuid = lineUuid
        this.name = name
        this.description = description
        this.quantity = quantity
        this.price = price
    }
}
