package io.provenance.invoice.util.provenance

import com.google.protobuf.ByteString
import cosmos.crypto.secp256k1.Keys
import io.provenance.client.grpc.Signer
import io.provenance.hdwallet.ec.toECKeyPair
import io.provenance.hdwallet.ec.toECPrivateKey
import io.provenance.hdwallet.signer.BCECSigner
import io.provenance.scope.util.sha256
import java.security.PrivateKey

class KeySigner(private val address: String, privateKey: PrivateKey): Signer {
    private val keyPair = privateKey.toECPrivateKey().toECKeyPair()

    override fun address(): String = address

    override fun pubKey(): Keys.PubKey =
        Keys.PubKey.newBuilder().setKey(ByteString.copyFrom(keyPair.publicKey.compressed())).build()

    override fun sign(data: ByteArray): ByteArray = BCECSigner()
        .sign(keyPair.privateKey, data.sha256())
        .encodeAsBTC()
}
