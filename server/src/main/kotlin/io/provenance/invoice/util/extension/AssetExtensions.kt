package io.provenance.invoice.util.extension

import com.google.protobuf.DescriptorProtos.EnumValueOptions
import com.google.protobuf.GeneratedMessage.GeneratedExtension
import com.google.protobuf.MessageOrBuilder
import com.google.protobuf.ProtocolMessageEnum
import io.provenance.invoice.AssetProtos
import io.provenance.invoice.AssetProtos.Asset
import io.provenance.invoice.AssetProtos.AssetOrBuilder
import io.provenance.invoice.AssetProtos.AssetType
import io.provenance.invoice.AssetProtosBuilders
import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.InvoiceProtos.InvoiceOrBuilder
import io.provenance.invoice.UtilProtos
import io.provenance.invoice.util.randomProtoUuid
import java.math.BigDecimal

fun AssetType.provenanceName(): String = this.getExtensionValue(AssetProtos.provenanceName)

fun AssetType.assetKvName(): String = this.getExtensionValue(AssetProtos.assetKvName)

fun InvoiceOrBuilder.toAsset(): Asset = this.toAsset(
    assetType = AssetType.NFT,
    assetDescription = "${AssetType.NFT.provenanceName()} [${this.invoiceUuid.value}]",
    idProvider = { invoiceUuid },
)

fun AssetOrBuilder.unpackInvoice(): Invoice = this
    .check({ it.type == AssetType.NFT.name }) { "Cannot unpack invoice from asset. Expected invoice to be properly typed as [${AssetType.NFT.name}] but type was [$type]" }
    .kvMap[AssetType.NFT.assetKvName()]
    .checkNotNull { "Expected the NFT to be serialized into the KV map of the asset under its KV name of [${AssetType.NFT.assetKvName()}]" }
    .unpack(Invoice::class.java)

private fun <T: MessageOrBuilder> T.toAsset(
    assetType: AssetType,
    assetDescription: String = assetType.provenanceName(),
    idProvider: (T) -> UtilProtos.UUID = { randomProtoUuid() },
): Asset = this.let { message ->
    AssetProtosBuilders.Asset {
        id = idProvider.invoke(message)
        type = assetType.name
        description = assetDescription
        putKv(assetType.assetKvName(), message.toProtoAny())
    }
}

private fun <T: ProtocolMessageEnum, U: Any> T.getExtensionValue(extension: GeneratedExtension<EnumValueOptions, U>): U =
    this.valueDescriptor.options.getExtension(extension)

fun InvoiceOrBuilder.totalAmount(): BigDecimal = lineItemsList.sumOf { it.quantity.toBigDecimal() * it.price.toBigDecimalOrZero() }
