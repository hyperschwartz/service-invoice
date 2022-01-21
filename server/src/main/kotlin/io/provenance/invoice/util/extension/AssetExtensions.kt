package io.provenance.invoice.util.extension

import com.google.protobuf.DescriptorProtos.EnumValueOptions
import com.google.protobuf.GeneratedMessage.GeneratedExtension
import com.google.protobuf.MessageOrBuilder
import com.google.protobuf.ProtocolMessageEnum
import io.provenance.invoice.AssetProtos
import io.provenance.invoice.AssetProtos.Asset
import io.provenance.invoice.AssetProtos.AssetOrBuilder
import io.provenance.invoice.AssetProtos.AssetType
import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.InvoiceProtos.InvoiceOrBuilder
import io.provenance.invoice.util.randomProtoUuid

fun AssetType.provenanceName(): String = this.getExtensionValue(AssetProtos.provenanceName)

fun AssetType.assetKvName(): String = this.getExtensionValue(AssetProtos.assetKvName)

fun InvoiceOrBuilder.toAsset(): Asset = this.toAsset(
    type = AssetType.NFT,
    description = "${AssetType.NFT.provenanceName()} [${this.invoiceUuid.value}]",
)

fun AssetOrBuilder.unpackInvoice(): Invoice = this
    .check({ it.type == AssetType.NFT.name }) { "Cannot unpack invoice from asset. Expected invoice to be properly typed as [${AssetType.NFT.name}] but type was [$type]" }
    .kvMap[AssetType.NFT.assetKvName()]
    .checkNotNull { "Expected the NFT to be serialized into the KV map of the asset under its KV name of [${AssetType.NFT.assetKvName()}]" }
    .unpack(Invoice::class.java)

private fun <T: MessageOrBuilder> T.toAsset(
    type: AssetType,
    description: String = type.provenanceName(),
): Asset = Asset.newBuilder().also { builder ->
    builder.id = randomProtoUuid()
    builder.type = type.name
    builder.description = description
    // TODO: Might need to swap to the commented-out strategy based on how development with onboarding api goes
    builder.putKv(type.assetKvName(), this.toProtoAny())
//    this.allFields.forEach { (descriptor, value) ->
//        builder.putKv(descriptor.name, value.genericToProtoAny())
//    }
}.build()

private fun <T: ProtocolMessageEnum, U: Any> T.getExtensionValue(extension: GeneratedExtension<EnumValueOptions, U>): U =
    this.valueDescriptor.options.getExtension(extension)
