package io.provenance.invoice.util.extension

import helper.MockProtoUtil
import helper.assertSucceeds
import io.provenance.invoice.AssetProtos.AssetType
import io.provenance.invoice.AssetProtosBuilders
import io.provenance.invoice.util.randomProtoUuidI
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssetExtensionsTest {
    @Test
    fun testUnpackInvoiceFromAsset() {
        val invoice = MockProtoUtil.getMockInvoice()
        val asset = AssetProtosBuilders.Asset {
            id = randomProtoUuidI()
            type = AssetType.NFT.name
            description = "Doesn't matter"
            putKv(AssetType.NFT.assetKvNameI(), invoice.toProtoAnyI())
        }
        val unpackedInvoice = assertSucceeds("Expected the invoice kv to successfully unpack to an Invoice proto") {
            asset.unpackInvoiceI()
        }
        assertEquals(expected = invoice, actual = unpackedInvoice, "The asset's packed invoice should properly deserialize into the original")
    }

    @Test
    fun testPackInvoiceAsAsset() {
        val invoice = MockProtoUtil.getMockInvoice()
        val asset = invoice.toAssetI()
        assertTrue(actual = asset.id.isSet(), message = "Expected the newly-created asset to get an id")
        assertEquals(expected = asset.type, actual = AssetType.NFT.name, message = "Expected the type to equate to the name of the NFT asset type enum")
        assertTrue(actual = asset.description.isNotBlank(), message = "Expected the description to be established")
        assertEquals(expected = 1, actual = asset.kvCount, "Expected only one KV to be set")
        val unpackedInvoice = assertSucceeds("Expected the invoice kv to successfully unpack to an Invoice proto") {
            asset.unpackInvoiceI()
        }
        assertEquals(expected = invoice, actual = unpackedInvoice, "The asset's packed invoice should properly deserialize into the original")
    }
}
