package helper.calc

import helper.calc.TestLineItem.TestLineItemBuilder
import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.InvoiceProtos.LineItem
import io.provenance.invoice.util.extension.toProtoDateI
import io.provenance.invoice.util.extension.toProtoDecimalI
import io.provenance.invoice.util.extension.toProtoUuidI
import io.provenance.invoice.util.extension.wrapListI
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class TestInvoice internal constructor(
    val invoiceUuid: UUID,
    val fromAddress: String,
    val toAddress: String,
    val createdDate: LocalDate,
    val dueDate: LocalDate,
    val description: String,
    val paymentDenom: String,
    val lineItems: List<TestLineItem>,
) {
    companion object {
        fun builder(): TestInvoiceBuilder = TestInvoiceBuilder()

        fun default(): TestInvoice = TestInvoiceBuilder().build()
    }

    class TestInvoiceBuilder internal constructor() {
        private var invoiceUuid: UUID? = null
        private var fromAddress: String? = null
        private var toAddress: String? = null
        private var createdDate: LocalDate? = null
        private var dueDate: LocalDate? = null
        private var description: String? = null
        private var paymentDenom: String? = null
        private var lineItems: MutableList<TestLineItem> = mutableListOf()

        fun invoiceUuid(invoiceUuid: UUID) = apply { this.invoiceUuid = invoiceUuid }
        fun fromAddress(fromAddress: String) = apply { this.fromAddress = fromAddress }
        fun toAddress(toAddress: String) = apply { this.toAddress = toAddress }
        fun createdDate(createdDate: LocalDate) = apply { this.createdDate = createdDate }
        fun dueDate(dueDate: LocalDate) = apply { this.dueDate = dueDate }
        fun description(description: String) = apply { this.description = description }
        fun paymentDenom(paymentDenom: String) = apply { this.paymentDenom = paymentDenom }
        fun addLineItem(lineItem: TestLineItem) = apply { this.lineItems.add(lineItem) }
        fun addLineItemBuilder(fn: (TestLineItemBuilder) -> TestLineItemBuilder) = apply { this.lineItems.add(fn(TestLineItemBuilder()).build()) }

        fun build(validate: Boolean = true): TestInvoice = TestInvoice(
            invoiceUuid = this.invoiceUuid ?: UUID.randomUUID(),
            fromAddress = this.fromAddress ?: "sender",
            toAddress = this.toAddress ?: "receiver",
            createdDate = this.createdDate ?: LocalDate.now(),
            dueDate = this.dueDate ?: LocalDate.now().plusMonths(3),
            description = this.description ?: "Invoice for money receiving",
            paymentDenom = this.paymentDenom ?: "nhash",
            lineItems = this.lineItems.takeUnless { it.isEmpty() } ?: TestLineItem.default().wrapListI(),
        )
    }

    fun toProto(): Invoice = Invoice.newBuilder().also { invoiceBuilder ->
        invoiceBuilder.invoiceUuid = invoiceUuid.toProtoUuidI()
        invoiceBuilder.fromAddress = fromAddress
        invoiceBuilder.toAddress = toAddress
        invoiceBuilder.invoiceCreatedDate = createdDate.toProtoDateI()
        invoiceBuilder.invoiceDueDate = dueDate.toProtoDateI()
        invoiceBuilder.description = description
        invoiceBuilder.paymentDenom = paymentDenom
        invoiceBuilder.addAllLineItems(lineItems.map { it.toProto() })
    }.build()
}

data class TestLineItem internal constructor(
    val lineUuid: UUID,
    val name: String,
    val description: String,
    val quantity: Int,
    val price: BigDecimal,
) {
    companion object {
        fun builder(): TestLineItemBuilder = TestLineItemBuilder()

        fun default(): TestLineItem = TestLineItemBuilder().build()
    }

    class TestLineItemBuilder internal constructor() {
        private var lineUuid: UUID? = null
        private var name: String? = null
        private var description: String? = null
        private var quantity: Int? = null
        private var price: BigDecimal? = null

        fun lineUuid(lineUuid: UUID) = apply { this.lineUuid = lineUuid }
        fun name(name: String) = apply { this.name = name }
        fun description(description: String) = apply { this.description = description }
        fun quantity(quantity: Int) = apply { this.quantity = quantity }
        fun price(price: BigDecimal) = apply { this.price = price }

        fun build(): TestLineItem = TestLineItem(
            lineUuid = this.lineUuid ?: UUID.randomUUID(),
            name = this.name ?: "Payment for Things",
            description = this.description ?: "A requirement is that things are paid",
            quantity = quantity ?: 1,
            price = price ?: "100".toBigDecimal(),
        )
    }

    fun toProto(): LineItem = LineItem.newBuilder().also { itemBuilder ->
        itemBuilder.lineUuid = lineUuid.toProtoUuidI()
        itemBuilder.name = name
        itemBuilder.description = description
        itemBuilder.quantity = quantity
        itemBuilder.price = price.toProtoDecimalI()
    }.build()
}
