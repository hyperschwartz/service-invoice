package io.provenance.invoice.util.extension

import helper.assertSucceeds
import io.provenance.invoice.util.mock.MockInvoice
import org.junit.jupiter.api.Test
import tech.figure.asset.v1beta1.Asset
import tech.figure.util.v1beta1.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssetExtensionsTest {
    @Test
    fun testAssetToInvoice() {
        val invoice = MockInvoice.defaultProto()
        val asset = Asset.newBuilder().also { assetBuilder ->
            assetBuilder.id = UUID.newBuilder().setValue(invoice.invoiceUuid.value).build()
            assetBuilder.type = "payable"
            assetBuilder.description = "Invoice [${invoice.invoiceUuid.value}]"
            assetBuilder.putKv("invoice", invoice.toProtoAnyI())
        }.build()
        val unpackedInvoice = assertSucceeds("Expected the invoice kv to successfully unpack to an Invoice proto") {
            asset.toInvoiceI()
        }
        assertEquals(expected = invoice, actual = unpackedInvoice, "The asset's packed invoice should properly deserialize into the original")
    }

    @Test
    fun testInvoiceToAsset() {
        val invoice = MockInvoice.defaultProto()
        val asset = invoice.toAssetI()
        assertTrue(actual = asset.id.isSet(), message = "Expected the newly-created asset to get an id")
        assertEquals(expected = asset.type, actual = "payable", message = "Expected the type to equate to the payable type")
        assertTrue(actual = asset.description.isNotBlank(), message = "Expected the description to be established")
        assertEquals(expected = 1, actual = asset.kvCount, "Expected only one KV to be set")
        val unpackedInvoice = assertSucceeds("Expected the invoice kv to successfully unpack to an Invoice proto") {
            asset.toInvoiceI()
        }
        assertEquals(expected = invoice, actual = unpackedInvoice, "The asset's packed invoice should properly deserialize into the original")
    }
}
