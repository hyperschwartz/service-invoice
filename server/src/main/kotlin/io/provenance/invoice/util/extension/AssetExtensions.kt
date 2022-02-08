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
import io.provenance.invoice.util.randomProtoUuidI
import java.math.BigDecimal

fun AssetType.provenanceNameI(): String = this.getExtensionValue(AssetProtos.provenanceName)

fun AssetType.assetKvNameI(): String = this.getExtensionValue(AssetProtos.assetKvName)

fun InvoiceOrBuilder.toAssetI(): Asset = this.toAssetI(
    assetType = AssetType.NFT,
    assetDescription = "Invoice [${this.invoiceUuid.value}]",
    idProvider = { invoiceUuid },
)

fun InvoiceOrBuilder.totalAmountI(): BigDecimal = lineItemsList.sumOf { it.quantity.toBigDecimal() * it.price.toBigDecimalOrZeroI() }

fun AssetOrBuilder.unpackInvoiceI(): Invoice = this
    .checkI({ it.type == AssetType.NFT.name }) { "Cannot unpack invoice from asset. Expected invoice to be properly typed as [${AssetType.NFT.name}] but type was [$type]" }
    .kvMap[AssetType.NFT.assetKvNameI()]
    .checkNotNullI { "Expected the NFT to be serialized into the KV map of the asset under its KV name of [${AssetType.NFT.assetKvNameI()}]" }
    .unpack(Invoice::class.java)

private fun <T: MessageOrBuilder> T.toAssetI(
    assetType: AssetType,
    assetDescription: String = assetType.provenanceNameI(),
    idProvider: (T) -> UtilProtos.UUID = { randomProtoUuidI() },
): Asset = this.let { message ->
    AssetProtosBuilders.Asset {
        id = idProvider.invoke(message)
        type = assetType.name
        description = assetDescription
        putKv(assetType.assetKvNameI(), message.toProtoAnyI())
    }
}

private fun <T: ProtocolMessageEnum, U: Any> T.getExtensionValue(extension: GeneratedExtension<EnumValueOptions, U>): U =
    this.valueDescriptor.options.getExtension(extension)


