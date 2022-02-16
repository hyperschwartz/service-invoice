package io.provenance.invoice.util.provenance

import com.google.protobuf.ByteString
import cosmos.crypto.secp256k1.Keys
import io.provenance.client.grpc.Signer
import io.provenance.hdwallet.ec.PrivateKey
import io.provenance.hdwallet.ec.PublicKey
import io.provenance.hdwallet.ec.extensions.toECPrivateKey
import io.provenance.hdwallet.ec.extensions.toJavaECPrivateKey
import io.provenance.hdwallet.signer.BCECSigner
import io.provenance.scope.encryption.util.getAddress
import io.provenance.scope.encryption.util.toKeyPair
import io.provenance.scope.util.sha256

class AccountSigner(
    private val address: String,
    private val publicKey: PublicKey,
    private val privateKey: PrivateKey
): Signer {
    override fun address(): String = address

    override fun pubKey(): Keys.PubKey =
        Keys.PubKey.newBuilder().setKey(ByteString.copyFrom(publicKey.compressed())).build()

    override fun sign(data: ByteArray): ByteArray = BCECSigner().sign(privateKey, data.sha256()).encodeAsBTC().toByteArray()

    companion object {
        fun fromAccountDetail(
            accountDetail: ProvenanceAccountDetail
        ): AccountSigner = accountDetail.privateKey.toECPrivateKey().toECKeyPair().let { keyPair ->
            AccountSigner(
                address = accountDetail.bech32Address,
                publicKey = keyPair.publicKey,
                privateKey = keyPair.privateKey,
            )
        }

        fun fromJavaPrivateKey(
            privateKey: java.security.PrivateKey,
            mainNet: Boolean,
        ): AccountSigner = fromWalletPrivateKey(privateKey.toECPrivateKey(), mainNet)

        fun fromWalletPrivateKey(
            privateKey: PrivateKey,
            mainNet: Boolean,
        ): AccountSigner = privateKey.toJavaECPrivateKey().toKeyPair().let { keyPair ->
            AccountSigner(
                address = keyPair.public.getAddress(mainNet),
                publicKey = privateKey.toPublicKey(),
                privateKey = privateKey,
            )
        }
    }
}
