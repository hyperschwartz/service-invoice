package helper

import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.InvoiceProtos.LineItem
import io.provenance.invoice.InvoiceProtosBuilders
import io.provenance.invoice.UtilProtos
import io.provenance.invoice.util.enums.ExpectedDenom
import io.provenance.invoice.util.extension.toProtoDateI
import io.provenance.invoice.util.extension.toProtoDecimalI
import io.provenance.invoice.util.randomProtoUuidI
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertTrue

object MockInvoiceUtil {
    // The onboarding code validates that this address is a real Bech32 format
    const val DEFAULT_FROM_ADDRESS: String = "tp1hyt8cwsqpgeajjxy92098tn27uyuqp25d30rdv"
    const val DEFAULT_TO_ADDRESS: String = "receiver"
    val DEFAULT_PAYMENT_DENOM: String = ExpectedDenom.NHASH.expectedName
    const val DEFAULT_LINE_NAME: String = "Money Request"
    const val DEFAULT_LINE_DESCRIPTION: String = "SHOW ME THE MONEY"
    const val DEFAULT_LINE_QUANTITY: Int = 1
    val DEFAULT_LINE_PRICE: BigDecimal = "100".toBigDecimal()


    fun getMockInvoice(lineItemAmount: Int = 1): Invoice = InvoiceProtosBuilders.Invoice {
        // Ensure this value isn't being abused
        assertTrue(lineItemAmount >= 0, "Line item amount [$lineItemAmount] should be at least zero")
        invoiceUuid = randomProtoUuidI()
        fromAddress = DEFAULT_FROM_ADDRESS
        toAddress = DEFAULT_TO_ADDRESS
        invoiceCreatedDate = LocalDate.now().toProtoDateI()
        invoiceDueDate = LocalDate.now().plusMonths(1).toProtoDateI()
        description = "Test invoice ${UUID.randomUUID()}"
        paymentDenom = DEFAULT_PAYMENT_DENOM
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
        name: String = DEFAULT_LINE_NAME,
        description: String = DEFAULT_LINE_DESCRIPTION,
        quantity: Int = DEFAULT_LINE_QUANTITY,
        price: UtilProtos.Decimal = DEFAULT_LINE_PRICE.toProtoDecimalI(),
    ): LineItem = InvoiceProtosBuilders.LineItem {
        this.lineUuid = lineUuid
        this.name = name
        this.description = description
        this.quantity = quantity
        this.price = price
    }
}
