package io.provenance.invoice.services.mock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.io.BaseEncoding
import cosmos.tx.v1beta1.TxOuterClass.TxBody
import io.provenance.invoice.AssetProtos.Asset
import io.provenance.invoice.clients.OnboardingResponse
import io.provenance.invoice.util.extension.toJsonProvenance
import io.provenance.invoice.util.extension.toProtoAny
import io.provenance.invoice.util.extension.toUuid
import io.provenance.invoice.util.extension.parseUuid
import io.provenance.metadata.v1.MsgWriteRecordRequest
import io.provenance.metadata.v1.MsgWriteScopeRequest
import io.provenance.metadata.v1.MsgWriteSessionRequest
import io.provenance.metadata.v1.Party
import io.provenance.metadata.v1.PartyType
import io.provenance.metadata.v1.RecordInput
import io.provenance.metadata.v1.RecordInputStatus
import io.provenance.metadata.v1.RecordOutput
import io.provenance.metadata.v1.ResultStatus
import io.provenance.scope.encryption.crypto.CertificateUtil
import io.provenance.scope.encryption.crypto.Pen
import io.provenance.scope.encryption.crypto.sign
import io.provenance.scope.encryption.dime.ProvenanceDIME
import io.provenance.scope.encryption.domain.inputstream.DIMEInputStream
import io.provenance.scope.encryption.ecies.ECUtils
import io.provenance.scope.encryption.ecies.ProvenanceKeyGenerator
import io.provenance.scope.objectstore.client.SIGNATURE_PUBLIC_KEY_FIELD_NAME
import io.provenance.scope.util.MetadataAddress
import io.provenance.scope.util.toByteString
import java.io.ByteArrayInputStream
import java.security.PublicKey
import java.util.Base64
import java.util.UUID

/**
 * Code is a hack to somewhat replicate the responses that service-asset-onboarding returns, without using object store.
 */
object AssetOnboardingMocker {
    private val SCOPE_SPEC_UUID = "551b5eca-921d-4ba7-aded-3966b224f44b".parseUuid()
    private val CONTRACT_SPEC_UUID = "f97ecc5d-c580-478d-be02-6c1b0c32235f".parseUuid()
    private const val RECORD_SPEC_NAME = "Asset"
    private val ASSET_SPEC_INPUT = RecordInputSpec(
        name = "AssetHash",
        typeName = "String",
        hash = "4B6A6C36E8B2622334C244B46799A47DBEAAF94E9D5B7637BC12A3A4988A62C0", // sha356(RecordSpecInputs.name)
    )
    private const val RECORD_PROCESS_NAME = "OnboardAssetProcess"
    private const val RECORD_PROCESS_METHOD = "OnboardAsset"
    private const val RECORD_PROCESS_HASH = "32D60974A2B2E9A9D9E93D9956E3A7D2BD226E1511D64D1EA39F86CBED62CE78" // sha356(RecordProcessMethod)

    /**
     * Core function.
     * Use this to dupe up an OnboardingResponse from an asset, public key, and address.  This will result in output
     * that will somewhat mimic the format derived via calling into serivce-asset-onboarding.
     *
     * NOTE: The input needs to have a valid public key to ensure signing functions don't throw exceptions, but the
     * input address can be anything.
     */
    fun mockAssetResponse(
        asset: Asset,
        publicKey: String,
        address: String,
    ): OnboardingResponse {
        val decodedPublicKey = ECUtils.convertBytesToPublicKey(BaseEncoding.base64().decode(publicKey))
        val hash = hashAsset(asset, decodedPublicKey)
        val txBody = buildTxBody(asset.id.toUuid(), hash, address)
        return OnboardingResponse(
            json = ObjectMapper().readValue(txBody.toJsonProvenance()),
            base64 = txBody.messagesList.map { it.toByteArray().toBase64String() },
        )
    }

    private fun hashAsset(asset: Asset, publicKey: PublicKey): String {
        val assetByteStream = asset.toByteArray().let(::ByteArrayInputStream)
        val signer = Pen(ProvenanceKeyGenerator.generateKeyPair(publicKey))
        val dime = ProvenanceDIME.createDIME(
            payload = assetByteStream.sign(signer),
            ownerEncryptionPublicKey = publicKey,
            processingAudienceKeys = emptyList(),
            sha256 = true,
        )
        val dimeInputStream = DIMEInputStream(
            dime = dime.dime,
            `in` = dime.encryptedPayload,
            uuid = UUID.randomUUID(),
            metadata = mapOf(SIGNATURE_PUBLIC_KEY_FIELD_NAME to CertificateUtil.publicKeyToPem(signer.getPublicKey())),
            internalHash = true,
            externalHash = false,
        )
        return dimeInputStream.use { it.internalHash() }.toBase64String()
    }

    private fun buildTxBody(assetUuid: UUID, assetHash: String, address: String): TxBody {
        val sessionUuid = UUID.randomUUID()
        val audiences = listOf(address)
        val party = Party.newBuilder().also { partyBuilder ->
            partyBuilder.address = address
            partyBuilder.role = PartyType.PARTY_TYPE_OWNER
        }.build()
        val messages: List<com.google.protobuf.Any> = listOf(
            // Write scope request to maintain the asset
            MsgWriteScopeRequest.newBuilder().also { msgBuilder ->
                msgBuilder.scopeUuid = assetUuid.toString()
                msgBuilder.specUuid = SCOPE_SPEC_UUID.toString()
                msgBuilder.scopeBuilder.also { scopeBuilder ->
                    scopeBuilder.scopeId = MetadataAddress.forScope(assetUuid).bytes.toByteString()
                    scopeBuilder.specificationId = MetadataAddress.forScopeSpecification(SCOPE_SPEC_UUID).bytes.toByteString()
                    scopeBuilder.valueOwnerAddress = address
                    scopeBuilder.addOwners(party)
                    scopeBuilder.addAllDataAccess(audiences)
                }
                msgBuilder.addSigners(address)
            }.build().toProtoAny(),
            // Write session request to serve the tx
            MsgWriteSessionRequest.newBuilder().also { msgBuilder ->
                msgBuilder.sessionIdComponentsBuilder.also { componentsBuilder ->
                    componentsBuilder.scopeUuid = assetUuid.toString()
                    componentsBuilder.sessionUuid = sessionUuid.toString()
                }
                msgBuilder.sessionBuilder.also { sessionBuilder ->
                    sessionBuilder.sessionId = MetadataAddress.forSession(assetUuid, sessionUuid).bytes.toByteString()
                    sessionBuilder.specificationId = MetadataAddress.forContractSpecification(CONTRACT_SPEC_UUID).bytes.toByteString()
                    sessionBuilder.addParties(party)
                    sessionBuilder.auditBuilder.createdBy = address
                    sessionBuilder.auditBuilder.updatedBy = address
                }
                msgBuilder.addSigners(address)
            }.build().toProtoAny(),
            // Write record address to mint the asset
            MsgWriteRecordRequest.newBuilder().also { msgBuilder ->
                msgBuilder.contractSpecUuid = CONTRACT_SPEC_UUID.toString()
                msgBuilder.recordBuilder.also { recordBuilder ->
                    recordBuilder.sessionId = MetadataAddress.forSession(assetUuid, sessionUuid).bytes.toByteString()
                    recordBuilder.specificationId = MetadataAddress.forRecordSpecification(CONTRACT_SPEC_UUID, RECORD_SPEC_NAME).bytes.toByteString()
                    recordBuilder.name = RECORD_SPEC_NAME
                    recordBuilder.addInputs(RecordInput.newBuilder().also { inputBuilder ->
                        inputBuilder.name = ASSET_SPEC_INPUT.name
                        inputBuilder.typeName = ASSET_SPEC_INPUT.typeName
                        inputBuilder.hash = assetHash
                        inputBuilder.status = RecordInputStatus.RECORD_INPUT_STATUS_PROPOSED
                    })
                    recordBuilder.addOutputs(RecordOutput.newBuilder().also { outputBuilder ->
                        outputBuilder.hash = assetHash
                        outputBuilder.status = ResultStatus.RESULT_STATUS_PASS
                    })
                    recordBuilder.processBuilder.also { processBuilder ->
                        processBuilder.name = RECORD_PROCESS_NAME
                        processBuilder.method = RECORD_PROCESS_METHOD
                        processBuilder.hash = RECORD_PROCESS_HASH
                    }
                }
                msgBuilder.addSigners(address)
            }.build().toProtoAny()
        )
        return TxBody.newBuilder().addAllMessages(messages).build()
    }

    private fun ByteArray.toBase64String(): String = Base64.getEncoder().encodeToString(this)

    private data class RecordInputSpec(
        val name: String,
        val typeName: String,
        val hash: String,
    )
}
