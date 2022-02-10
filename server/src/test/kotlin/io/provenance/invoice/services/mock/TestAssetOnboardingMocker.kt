package io.provenance.invoice.services.mock

import helper.MockInvoiceUtil
import helper.TestConstants
import helper.assertSucceeds
import io.provenance.invoice.util.extension.toAssetI
import io.provenance.invoice.util.extension.toUuidI
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class TestAssetOnboardingMocker {
    @Test
    fun testAssetOnboardingMockerBadInput() {
        val asset = MockInvoiceUtil.getMockInvoice().toAssetI()
        assertFails("Attempting to use an invalid public key should fail") {
            AssetOnboardingMocker.mockAssetResponse(
                asset = asset,
                publicKey = "YOLO",
                address = TestConstants.VALID_ADDRESS,
            )
        }
    }

    @Test
    fun testAssetOnboardingMockerResponses() {
        val asset = MockInvoiceUtil.getMockInvoice().toAssetI()
        val assetUuid = asset.id.toUuidI()
        val response = assertSucceeds("A valid public key and address should produce output") {
            AssetOnboardingMocker.mockAssetResponse(
                asset = asset,
                publicKey = TestConstants.VALID_PUBLIC_KEY,
                address = TestConstants.VALID_ADDRESS,
            )
        }
        val writeScopeRequest = assertSucceeds("Expected the MsgWriteScopeRequest to be properly unpacked") { response.writeScopeRequest }
        assertEquals(
            expected = assetUuid.toString(),
            actual = writeScopeRequest.scopeUuid,
            message = "Expected the write scope request to respond with the asset uuid as the scope uuid",
        )
        val writeSessionRequest = assertSucceeds("Expected the MsgWriteSessionRequest to be properly unpacked") { response.writeSessionRequest }
        assertEquals(
            expected = assetUuid.toString(),
            actual = writeSessionRequest.sessionIdComponents.scopeUuid,
            message = "Expected the write session request to respond with the asset uuid as the scope uuid in its session id components",
        )
        assertSucceeds("Expected the MsgWriteRecordRequest to be properly unpacked") { response.writeRecordRequest }
    }
}
