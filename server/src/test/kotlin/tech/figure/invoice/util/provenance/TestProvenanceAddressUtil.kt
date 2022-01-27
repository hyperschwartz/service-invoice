package tech.figure.invoice.util.provenance

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TestProvenanceAddressUtil {
    @Test
    fun testExpectedOutputBasedOnChainEnvironment() {
        val sourceAddress = "tp172yscg9eu72hknhue4sae5z3yyddxlfsfntcys"
        val sampleDenom = "invoice-816c4384-7f8c-11ec-949d-afb68037fcad"
        assertEquals(
            expected = "tp1hyt8cwsqpgeajjxy92098tn27uyuqp25d30rdv",
            actual = ProvenanceAddressUtil.generateMarkerAddressForDenomFromSource(
                denom = sampleDenom,
                accountAddress = sourceAddress,
            ),
            message = "Expected the proper output when a tp address is provided",
        )
    }

    @Test
    fun testExpectedNhashOutput() {
        assertEquals(
            expected = "tp1pr93cqdh4kfnmrknhwa87a5qrwxw9k3dhkszp0",
            actual = ProvenanceAddressUtil.generateMarkerAddressForDenom("nhash", "tp"),
            message = "Expected to see the proper nhash address",
        )
    }
}
