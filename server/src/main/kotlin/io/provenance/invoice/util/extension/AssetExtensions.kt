package io.provenance.invoice.util.extension

import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.InvoiceProtos.InvoiceOrBuilder
import tech.figure.asset.v1beta1.Asset
import tech.figure.asset.v1beta1.AssetOrBuilder
import java.math.BigDecimal

private const val ASSET_TYPE = "payable"
private const val INVOICE_KV_NAME = "invoice"

fun InvoiceOrBuilder.toAssetI(): Asset = Asset.newBuilder().also { assetBuilder ->
    assetBuilder.id = tech.figure.util.v1beta1.UUID.newBuilder().setValue(this.invoiceUuid.value).build()
    assetBuilder.type = ASSET_TYPE
    assetBuilder.description = "Invoice [${this.invoiceUuid.value}]"
    assetBuilder.putKv(INVOICE_KV_NAME, this.toProtoAnyI())
}.build()

fun InvoiceOrBuilder.totalAmountI(): BigDecimal = lineItemsList.sumOf { it.quantity.toBigDecimal() * it.price.toBigDecimalOrZeroI() }

fun AssetOrBuilder.toInvoiceI(): Invoice = this
    .checkI(
        predicate = { asset -> asset.kvCount == 1 },
        lazyMessage = { "An asset containing an invoice should only contain a single kv value, but found keys: ${this.kvMap.keys}" },
    )
    .kvMap[INVOICE_KV_NAME]
    ?.unpack(Invoice::class.java)
    ?: throw IllegalStateException("No key of type [$INVOICE_KV_NAME] could be found in asset kv map.  Found keys: ${this.kvMap.keys}")
