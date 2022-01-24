package helper

import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.InvoiceProtos.LineItem
import io.provenance.invoice.UtilProtos
import io.provenance.invoice.util.extension.toProtoDate
import io.provenance.invoice.util.extension.toProtoDecimal
import io.provenance.invoice.util.randomProtoUuid
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertTrue

object MockProtoUtil {
    fun getMockInvoice(lineItemAmount: Int = 1): Invoice = Invoice.newBuilder().also { invoiceBuilder ->
        // Ensure this value isn't being abused
        assertTrue(lineItemAmount >= 0, "Line item amount [$lineItemAmount] should be at least zero")
        invoiceBuilder.invoiceUuid = randomProtoUuid()
        invoiceBuilder.fromAddress = "sender"
        invoiceBuilder.toAddress = "receiver"
        invoiceBuilder.invoiceCreatedDate = LocalDate.now().toProtoDate()
        invoiceBuilder.invoiceDueDate = LocalDate.now().plusMonths(1).toProtoDate()
        invoiceBuilder.description = "Test invoice ${UUID.randomUUID()}"
        invoiceBuilder.paymentDenom = "nhash"
        for (i in 0 until lineItemAmount) {
            val iteration = i.plus(1).toBigDecimal()
            invoiceBuilder.addLineItems(
                // Slightly differentiate each item based on the number of items
                this.getMockLineItem(
                    description = "Generated item [$iteration]",
                    quantity = iteration.toInt(),
                    price = "10".toBigDecimal().times(iteration).toProtoDecimal(),
                )
            )
        }
    }.build()

    fun getMockLineItem(
        lineUuid: UtilProtos.UUID = randomProtoUuid(),
        name: String = "money request",
        description: String = "GIMME THAT MONEY",
        quantity: Int = 1,
        price: UtilProtos.Decimal = "100".toBigDecimal().toProtoDecimal(),
    ): LineItem = LineItem.newBuilder().also { itemBuilder ->
        itemBuilder.lineUuid = lineUuid
        itemBuilder.name = name
        itemBuilder.description = description
        itemBuilder.quantity = quantity
        itemBuilder.price = price
    }.build()
}
