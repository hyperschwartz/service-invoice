package io.provenance.invoice.util.provenance

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

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
}
