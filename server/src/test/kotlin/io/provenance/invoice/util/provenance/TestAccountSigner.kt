package io.provenance.invoice.util.provenance

import helper.assertSucceeds
import io.provenance.hdwallet.ec.extensions.toECPrivateKey
import io.provenance.scope.encryption.ecies.ECUtils
import org.junit.jupiter.api.Test
import org.testcontainers.shaded.com.google.common.io.BaseEncoding
import kotlin.test.assertEquals

class TestAccountSigner {
    private companion object {
        const val VALID_TEST_NET_ADDRESS = "tp1m3tnt37gk63aqd0t5wwm4kz8t0w8sqegzg00sy"
        const val VALID_MAIN_NET_ADDRESS = "pb1m3tnt37gk63aqd0t5wwm4kz8t0w8sqeg3r2rjw"
        const val VALID_PRIVATE_KEY_ENCODED = "ALtrz4iQKL/mkpdLbUjS9fSkPsZaykR1HYImKn2YM2fP"
    }

    @Test
    fun testFromAccountDetail() {
        testAccountSigner("fromBase64PrivateKey") { mainNet ->
            AccountSigner.fromAccountDetail(
                ProvenanceAccountDetail.fromBase64PrivateKey(
                    privateKeyEncoded = VALID_PRIVATE_KEY_ENCODED,
                    mainNet = mainNet,
                )
            )
        }
    }

    @Test
    fun testFromJavaPrivateKey() {
        testAccountSigner("fromJavaPrivateKey") { mainNet ->
            AccountSigner.fromJavaPrivateKey(
                privateKey = ECUtils.convertBytesToPrivateKey(BaseEncoding.base64().decode(VALID_PRIVATE_KEY_ENCODED)),
                mainNet = mainNet,
            )
        }
    }

    @Test
    fun testFromWalletPrivateKey() {
        testAccountSigner("fromWalletPrivateKey") { mainNet ->
            AccountSigner.fromWalletPrivateKey(
                privateKey = ECUtils.convertBytesToPrivateKey(BaseEncoding.base64().decode(VALID_PRIVATE_KEY_ENCODED)).toECPrivateKey(),
                mainNet = mainNet,
            )
        }
    }

    private fun testAccountSigner(message: String, signerFn: (mainNet: Boolean) -> AccountSigner) {
        listOf(false, true).forEach { mainNet ->
            val errorPrefix = "$message [MainNet? $mainNet]:"
            val signer = assertSucceeds("$errorPrefix Expected to be able to create a signer") { signerFn(mainNet) }
            assertEquals(
                expected = if (mainNet) VALID_MAIN_NET_ADDRESS else VALID_TEST_NET_ADDRESS,
                actual = signer.address(),
                message = "$errorPrefix: Expected the address output to be correct",
            )
            assertSucceeds("$errorPrefix: Expected pubKey generation to succeed") { signer.pubKey() }
            assertSucceeds("$errorPrefix: Expected signing of abritrary data should succeed") {
                signer.sign("testdata".toByteArray())
            }
        }

    }
}
