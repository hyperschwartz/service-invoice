package io.provenance.invoice.util.provenance

import com.google.common.io.BaseEncoding
import io.provenance.scope.encryption.ecies.ECUtils
import io.provenance.scope.encryption.model.DirectKeyRef
import io.provenance.scope.encryption.model.KeyRef
import io.provenance.scope.encryption.util.getAddress
import io.provenance.scope.encryption.util.toKeyPair
import io.provenance.scope.objectstore.util.base64EncodeString
import java.security.PrivateKey
import java.security.PublicKey

data class ProvenanceAccountDetail(
    val bech32Address: String,
    val publicKey: PublicKey,
    val privateKey: PrivateKey,
    val encodedPublicKey: String,
    val encodedPrivateKey: String,
    val keyRef: KeyRef,
) {
    companion object {
        fun fromBase64PrivateKey(privateKeyEncoded: String, mainNet: Boolean): ProvenanceAccountDetail =
            ECUtils.convertBytesToPrivateKey(BaseEncoding.base64().decode(privateKeyEncoded)).toKeyPair().let { keyPair ->
                ProvenanceAccountDetail(
                    bech32Address = keyPair.public.getAddress(mainNet),
                    publicKey = keyPair.public,
                    privateKey = keyPair.private,
                    encodedPublicKey = ECUtils.convertPublicKeyToBytes(keyPair.public).base64EncodeString(),
                    encodedPrivateKey = privateKeyEncoded,
                    keyRef = DirectKeyRef(keyPair),
                )
            }
    }
}
