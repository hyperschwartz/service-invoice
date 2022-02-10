package io.provenance.invoice.util.provenance

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestProvenanceUtil {
    private companion object {
        private const val SAMPLE_DENOM = "invoice-816c4384-7f8c-11ec-949d-afb68037fcad"
        private const val SOURCE_ADDRESS_BODY = "1hyt8cwsqpgeajjxy92098tn27uyuqp25"
    }

    @Test
    fun testExpectedOutputBasedOnSourceAddress() {
        val sourceAddress = "tp172yscg9eu72hknhue4sae5z3yyddxlfsfntcys"
        assertEquals(
            expected = "tp1hyt8cwsqpgeajjxy92098tn27uyuqp25d30rdv",
            actual = ProvenanceUtil.generateMarkerAddressForDenomFromSource(
                denom = SAMPLE_DENOM,
                accountAddress = sourceAddress,
            ),
            message = "Expected the proper output when a tp address is provided",
        )
    }

    @Test
    fun testExpectedNhashOutput() {
        assertEquals(
            expected = "tp1pr93cqdh4kfnmrknhwa87a5qrwxw9k3dhkszp0",
            actual = ProvenanceUtil.generateMarkerAddressForDenom("nhash", "tp"),
            message = "Expected to see the proper nhash address",
        )
    }

    @Test
    fun testExpectedValuesBasedOnHRPTestNet() {
        val expectedTestNetCheckSum = "d30rdv"
        assertEquals(
            expected = "tp$SOURCE_ADDRESS_BODY$expectedTestNetCheckSum",
            actual = ProvenanceUtil.generateMarkerAddressForDenom(SAMPLE_DENOM, "tp"),
            message = "Expected the testnet output to be derived with the appropriate algorithm",
        )
    }

    @Test
    fun testExpectedValuesBasedOnHRPMainNet() {
        val expectedMainNetCheckSum = "76200x"
        assertEquals(
            expected = "pb$SOURCE_ADDRESS_BODY$expectedMainNetCheckSum",
            actual = ProvenanceUtil.generateMarkerAddressForDenom(SAMPLE_DENOM, "pb"),
            message = "Expected the mainnet output to be derived with the appropriate algorithm",
        )
    }

    @Test
    fun testIsBech32AddressValid() {
        val validTestNetAddress = "tp${SOURCE_ADDRESS_BODY}d30rdv"
        val validMainNetAddress = "pb${SOURCE_ADDRESS_BODY}76200x"
        assertTrue(
            actual = ProvenanceUtil.isBech32AddressValid(validTestNetAddress),
            message = "Expected a valid testnet address to return true",
        )
        assertTrue(
            actual = ProvenanceUtil.isBech32AddressValid(validMainNetAddress),
            message = "Expected a valid mainnet address to return true",
        )
        assertFalse(
            actual = ProvenanceUtil.isBech32AddressValid("fakestuff"),
            message = "Expected an invalid address to return false",
        )
        assertFalse(
            actual = ProvenanceUtil.isBech32AddressValid("tpblahblahblahblah"),
            message = "Expected an invalid testnet address to return false",
        )
        assertFalse(
            actual = ProvenanceUtil.isBech32AddressValid("pbblahblahblahblah"),
            message = "Expected an invalid mainnet address to return false",
        )
    }
}
