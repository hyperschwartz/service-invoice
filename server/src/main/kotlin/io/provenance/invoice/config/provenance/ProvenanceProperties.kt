package io.provenance.invoice.config.provenance

import com.google.common.io.BaseEncoding
import io.provenance.scope.encryption.ecies.ECUtils
import io.provenance.scope.encryption.util.toKeyPair
import io.provenance.scope.objectstore.util.base64EncodeString
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import java.net.URI
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey

@ConstructorBinding
@ConfigurationProperties(prefix = "provenance")
@Validated
data class ProvenanceProperties(
    val chainId: String,
    val channelUri: URI,
    val oraclePrivateKeyEncoded: String,
    val objectStoreUri: URI,
    val objectStoreTimeoutMs: Long,
) {
    // Decode oracle private key to a java security private key value with the handy-dandy ECUtils
    val oraclePrivateKey: PrivateKey by lazy {
        ECUtils.convertBytesToPrivateKey(BaseEncoding.base64().decode(oraclePrivateKeyEncoded))
    }

    val oracleKeyPair: KeyPair by lazy { oraclePrivateKey.toKeyPair() }

    val oraclePublicKeyEncoded: String by lazy {
        ECUtils.convertPublicKeyToBytes(oracleKeyPair.public).base64EncodeString()
    }

    val oraclePublicKey: PublicKey by lazy { oracleKeyPair.public }
}
