package helper

import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.InvoiceProtos.LineItem
import io.provenance.invoice.InvoiceProtosBuilders
import io.provenance.invoice.UtilProtos
import io.provenance.invoice.util.enums.ExpectedDenom
import io.provenance.invoice.util.extension.toProtoDateI
import io.provenance.invoice.util.extension.toProtoDecimalI
import io.provenance.invoice.util.randomProtoUuidI
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertTrue

object MockProtoUtil {
    fun getMockInvoice(lineItemAmount: Int = 1): Invoice = InvoiceProtosBuilders.Invoice {
        // Ensure this value isn't being abused
        assertTrue(lineItemAmount >= 0, "Line item amount [$lineItemAmount] should be at least zero")
        invoiceUuid = randomProtoUuidI()
        fromAddress = "sender"
        toAddress = "receiver"
        invoiceCreatedDate = LocalDate.now().toProtoDateI()
        invoiceDueDate = LocalDate.now().plusMonths(1).toProtoDateI()
        description = "Test invoice ${UUID.randomUUID()}"
        paymentDenom = ExpectedDenom.NHASH.expectedName
        for (i in 0 until lineItemAmount) {
            val iteration = i.plus(1).toBigDecimal()
            addLineItems(getMockLineItem(
                description = "Generated item [$iteration]",
                quantity = iteration.toInt(),
                price = "10".toBigDecimal().times(iteration).toProtoDecimalI(),
            ))
        }
    }

    fun getMockLineItem(
        lineUuid: UtilProtos.UUID = randomProtoUuidI(),
        name: String = "money request",
        description: String = "GIMME THAT MONEY",
        quantity: Int = 1,
        price: UtilProtos.Decimal = "100".toBigDecimal().toProtoDecimalI(),
    ): LineItem = InvoiceProtosBuilders.LineItem {
        this.lineUuid = lineUuid
        this.name = name
        this.description = description
        this.quantity = quantity
        this.price = price
    }
}
