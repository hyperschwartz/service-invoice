package io.provenance.invoice.util.mock

import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.InvoiceProtos.LineItem
import io.provenance.invoice.domain.dto.InvoiceDto
import io.provenance.invoice.util.enums.InvoiceStatus
import io.provenance.invoice.util.extension.toOffsetDateTimeI
import io.provenance.invoice.util.extension.toProtoDateI
import io.provenance.invoice.util.extension.toProtoDecimalI
import io.provenance.invoice.util.extension.toProtoUuidI
import io.provenance.invoice.util.extension.totalAmountI
import io.provenance.invoice.util.extension.wrapListI
import io.provenance.invoice.util.mock.MockLineItem.MockLineItemBuilder
import io.provenance.metadata.v1.MsgWriteRecordRequest
import io.provenance.metadata.v1.MsgWriteScopeRequest
import io.provenance.metadata.v1.MsgWriteSessionRequest
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class MockInvoice internal constructor(
    val invoiceUuid: UUID,
    val fromAddress: String,
    val toAddress: String,
    val createdDate: LocalDate,
    val dueDate: LocalDate,
    val description: String,
    val paymentDenom: String,
    val lineItems: List<MockLineItem>,
) {
    companion object {
        fun builder(): MockInvoiceBuilder = MockInvoiceBuilder()

        fun default(): MockInvoice = MockInvoiceBuilder().build()

        fun defaultProto(): Invoice = default().toProto()

        fun fromParams(params: MockInvoiceParams): MockInvoice = MockInvoice(
            invoiceUuid = params.invoiceUuid,
            fromAddress = params.fromAddress,
            toAddress = params.toAddress,
            createdDate = params.createdDate,
            dueDate = params.dueDate,
            description = params.description,
            paymentDenom = params.paymentDenom,
            lineItems = params.lineItems.map(MockLineItem::fromParams),
        )
    }

    class MockInvoiceBuilder internal constructor() {
        private var invoiceUuid: UUID? = null
        private var fromAddress: String? = null
        private var toAddress: String? = null
        private var createdDate: LocalDate? = null
        private var dueDate: LocalDate? = null
        private var description: String? = null
        private var paymentDenom: String? = null
        private var lineItems: MutableList<MockLineItem> = mutableListOf()

        // Only use the defaults if they are required and fields are unset
        private val defaults: MockInvoiceParams by lazy { MockInvoiceParams() }

        fun invoiceUuid(invoiceUuid: UUID) = apply { this.invoiceUuid = invoiceUuid }
        fun fromAddress(fromAddress: String) = apply { this.fromAddress = fromAddress }
        fun toAddress(toAddress: String) = apply { this.toAddress = toAddress }
        fun createdDate(createdDate: LocalDate) = apply { this.createdDate = createdDate }
        fun dueDate(dueDate: LocalDate) = apply { this.dueDate = dueDate }
        fun description(description: String) = apply { this.description = description }
        fun paymentDenom(paymentDenom: String) = apply { this.paymentDenom = paymentDenom }
        fun addLineForAmount(amount: BigDecimal) = apply { this.lineItems.add(MockLineItemBuilder().quantity(1).price(amount).build()) }
        fun addLineItem(lineItem: MockLineItem) = apply { this.lineItems.add(lineItem) }
        fun addLineItemBuilder(fn: (MockLineItemBuilder) -> MockLineItemBuilder) = apply { this.lineItems.add(fn(MockLineItemBuilder()).build()) }

        fun build(): MockInvoice = MockInvoice(
            invoiceUuid = this.invoiceUuid ?: defaults.invoiceUuid,
            fromAddress = this.fromAddress ?: defaults.fromAddress,
            toAddress = this.toAddress ?: defaults.toAddress,
            createdDate = this.createdDate ?: defaults.createdDate,
            dueDate = this.dueDate ?: defaults.dueDate,
            description = this.description ?: defaults.description,
            paymentDenom = this.paymentDenom ?: defaults.paymentDenom,
            lineItems = this.lineItems.takeUnless { it.isEmpty() } ?: defaults.lineItems.map(MockLineItem::fromParams),
        )

        fun buildProto(): Invoice = build().toProto()
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

    fun toDto(status: InvoiceStatus = InvoiceStatus.PENDING_STAMP): InvoiceDto = toProto().let { invoiceProto ->
        InvoiceDto(
            uuid = invoiceUuid,
            invoice = toProto(),
            status = status,
            totalOwed = invoiceProto.totalAmountI(),
            writeScopeRequest = MsgWriteScopeRequest.getDefaultInstance(),
            writeSessionRequest = MsgWriteSessionRequest.getDefaultInstance(),
            writeRecordRequest = MsgWriteRecordRequest.getDefaultInstance(),
            created = invoiceProto.invoiceCreatedDate.toOffsetDateTimeI(),
            updated = null,
        )
    }

    fun toCalcGen(): TestCalcGen = TestCalcGen.fromMockInvoice(this)

    fun toCustomCalcGen(startingInvoiceStatus: InvoiceStatus): TestCalcGen = TestCalcGen.fromMockInvoice(this, startingInvoiceStatus)
}

data class MockInvoiceParams(
    val invoiceUuid: UUID = UUID.randomUUID(),
    val fromAddress: String = MockInvoiceConstants.DEFAULT_FROM_ADDRESS,
    val toAddress: String = MockInvoiceConstants.DEFAULT_TO_ADDRESS,
    val createdDate: LocalDate = LocalDate.now(),
    val dueDate: LocalDate = LocalDate.now().plusMonths(3),
    val description: String = "Invoice for money receiving",
    val paymentDenom: String = MockInvoiceConstants.DEFAULT_PAYMENT_DENOM,
    val lineItems: List<MockLineItemParams> = MockLineItemParams().wrapListI(),
)

data class MockLineItem internal constructor(
    val lineUuid: UUID,
    val name: String,
    val description: String,
    val quantity: Int,
    val price: BigDecimal,
) {
    companion object {
        fun builder(): MockLineItemBuilder = MockLineItemBuilder()

        fun default(): MockLineItem = MockLineItemBuilder().build()

        fun defaultProto(): LineItem = default().toProto()

        fun fromParams(params: MockLineItemParams): MockLineItem = MockLineItem(
            lineUuid = params.lineUuid,
            name = params.name,
            description = params.name,
            quantity = params.quantity,
            price = params.price,
        )
    }

    class MockLineItemBuilder internal constructor() {
        private var lineUuid: UUID? = null
        private var name: String? = null
        private var description: String? = null
        private var quantity: Int? = null
        private var price: BigDecimal? = null

        // Only use the defaults if they are required and fields are unset
        private val defaults: MockLineItemParams by lazy { MockLineItemParams() }

        fun lineUuid(lineUuid: UUID) = apply { this.lineUuid = lineUuid }
        fun name(name: String) = apply { this.name = name }
        fun description(description: String) = apply { this.description = description }
        fun quantity(quantity: Int) = apply { this.quantity = quantity }
        fun price(price: BigDecimal) = apply { this.price = price }

        fun build(): MockLineItem = MockLineItem(
            lineUuid = this.lineUuid ?: defaults.lineUuid,
            name = this.name ?: defaults.name,
            description = this.description ?: defaults.description,
            quantity = quantity ?: defaults.quantity,
            price = price ?: defaults.price,
        )

        fun buildProto(): LineItem = build().toProto()
    }

    fun toProto(): LineItem = LineItem.newBuilder().also { itemBuilder ->
        itemBuilder.lineUuid = lineUuid.toProtoUuidI()
        itemBuilder.name = name
        itemBuilder.description = description
        itemBuilder.quantity = quantity
        itemBuilder.price = price.toProtoDecimalI()
    }.build()
}

data class MockLineItemParams(
    val lineUuid: UUID = UUID.randomUUID(),
    val name: String = MockInvoiceConstants.DEFAULT_LINE_NAME,
    val description: String = MockInvoiceConstants.DEFAULT_LINE_DESCRIPTION,
    val quantity: Int = MockInvoiceConstants.DEFAULT_LINE_QUANTITY,
    val price: BigDecimal = MockInvoiceConstants.DEFAULT_LINE_PRICE,
)
