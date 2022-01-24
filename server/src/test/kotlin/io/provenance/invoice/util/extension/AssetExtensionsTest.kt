package io.provenance.invoice.util.extension

import helper.MockProtoUtil
import helper.assertSucceeds
import io.provenance.invoice.AssetProtos.Asset
import io.provenance.invoice.AssetProtos.AssetType
import io.provenance.invoice.util.randomProtoUuid
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssetExtensionsTest {
    @Test
    fun testUnpackInvoiceFromAsset() {
        val invoice = MockProtoUtil.getMockInvoice()
        val asset = Asset.newBuilder().also { assetBuilder ->
            assetBuilder.id = randomProtoUuid()
            assetBuilder.type = AssetType.NFT.name
            assetBuilder.description = "Doesn't matter"
            assetBuilder.putKv(AssetType.NFT.assetKvName(), invoice.toProtoAny())
        }
        val unpackedInvoice = assertSucceeds("Expected the invoice kv to successfully unpack to an Invoice proto") {
            asset.unpackInvoice()
        }
        assertEquals(expected = invoice, actual = unpackedInvoice, "The asset's packed invoice should properly deserialize into the original")
    }

    @Test
    fun testPackInvoiceAsAsset() {
        val invoice = MockProtoUtil.getMockInvoice()
        val asset = invoice.toAsset()
        assertTrue(actual = asset.id.isSet(), message = "Expected the newly-created asset to get an id")
        assertEquals(expected = asset.type, actual = AssetType.NFT.name, message = "Expected the type to equate to the name of the NFT asset type enum")
        assertTrue(actual = asset.description.isNotBlank(), message = "Expected the description to be established")
        assertEquals(expected = 1, actual = asset.kvCount, "Expected only one KV to be set")
        val unpackedInvoice = assertSucceeds("Expected the invoice kv to successfully unpack to an Invoice proto") {
            asset.unpackInvoice()
        }
        assertEquals(expected = invoice, actual = unpackedInvoice, "The asset's packed invoice should properly deserialize into the original")
    }
}
