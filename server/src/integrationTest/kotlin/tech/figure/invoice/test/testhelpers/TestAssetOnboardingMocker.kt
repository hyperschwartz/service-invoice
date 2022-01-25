package tech.figure.invoice.test.testhelpers

import helper.MockProtoUtil
import helper.assertSucceeds
import org.junit.jupiter.api.Test
import tech.figure.invoice.testhelpers.AssetOnboardingMocker
import tech.figure.invoice.testhelpers.testcontainers.IntTestConstants
import tech.figure.invoice.util.extension.toAsset
import tech.figure.invoice.util.extension.toUuid
import kotlin.test.assertEquals
import kotlin.test.assertFails

class TestAssetOnboardingMocker {
    @Test
    fun testAssetOnboardingMockerBadInput() {
        val asset = MockProtoUtil.getMockInvoice().toAsset()
        assertFails("Attempting to use an invalid public key should fail") {
            AssetOnboardingMocker.mockAssetResponse(
                asset = asset,
                publicKey = "YOLO",
                address = IntTestConstants.VALID_ADDRESS,
            )
        }
    }

    @Test
    fun testAssetOnboardingMockerResponses() {
        val asset = MockProtoUtil.getMockInvoice().toAsset()
        val assetUuid = asset.id.toUuid()
        val response = assertSucceeds("A valid public key and address should produce output") {
            AssetOnboardingMocker.mockAssetResponse(
                asset = asset,
                publicKey = IntTestConstants.VALID_PUBLIC_KEY,
                address = IntTestConstants.VALID_ADDRESS,
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
