package io.provenance.invoice.services

import cosmos.tx.v1beta1.ServiceOuterClass
import cosmos.tx.v1beta1.TxOuterClass
import cosmwasm.wasm.v1.Tx
import io.provenance.client.PbClient
import io.provenance.client.grpc.BaseReq
import io.provenance.client.grpc.BaseReqSigner
import io.provenance.invoice.config.provenance.ObjectStore
import io.provenance.invoice.config.provenance.ProvenanceProperties
import io.provenance.invoice.domain.provenancetx.OracleApproval
import io.provenance.invoice.repository.InvoiceRepository
import io.provenance.invoice.util.enums.InvoiceStatus
import io.provenance.invoice.util.extension.checkNotNullI
import io.provenance.invoice.util.extension.toProtoAnyI
import io.provenance.invoice.util.extension.wrapListI
import io.provenance.invoice.util.provenance.AccountSigner
import io.provenance.name.v1.QueryResolveRequest
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ProvenanceQueryService(
    private val invoiceRepository: InvoiceRepository,
    private val objectStore: ObjectStore,
    private val pbClient: PbClient,
    private val provenanceProperties: ProvenanceProperties,
) {
    private companion object : KLogging()

    fun submitOracleApproval(invoiceUuid: UUID, logPrefix: String = "ORACLE APPROVAL [$invoiceUuid]:") {
        logger.info("$logPrefix Marking oracle approval on the chain")
        val contractInfo = try {
            pbClient.nameClient.resolve(QueryResolveRequest.newBuilder().setName(provenanceProperties.payablesContractName).build())
        } catch (e: Exception) {
            logger.error("$logPrefix Failed to resolve contract by name [${provenanceProperties.payablesContractName}]. Marking invoice with approval failure", e)
            approvalFailure(invoiceUuid)
            return
        }
        val baseAccount = try {
            pbClient.getBaseAccount(objectStore.oracleAccountDetail.bech32Address)
        } catch (e: Exception) {
            logger.error("$logPrefix Failed to fetch base account for oracle address [${objectStore.oracleAccountDetail.bech32Address}]. Marking invoice with approval failure", e)
            approvalFailure(invoiceUuid)
            return
        }
        val baseReq = BaseReq(
            signers = BaseReqSigner(
                signer = AccountSigner.fromAccountDetail(objectStore.oracleAccountDetail),
                sequenceOffset = 0,
                account = baseAccount,
            ).wrapListI(),
            body = TxOuterClass.TxBody.newBuilder().addMessages(
                Tx.MsgExecuteContract.newBuilder()
                    .setMsg(OracleApproval.forUuid(invoiceUuid).toBase64Msg())
                    .setContract(contractInfo.address)
                    .setSender(objectStore.oracleAccountDetail.bech32Address)
                    .build()
                    .toProtoAnyI()
            ).setMemo("Oracle signature").build(),
            chainId = provenanceProperties.chainId,
            gasAdjustment = 2.0,
        )
        val gasEstimate = try {
            pbClient.estimateTx(baseReq)
        } catch (e: Exception) {
            logger.error("$logPrefix Failed to estimate gas for oracle approval. Marking invoice with approval failure", e)
            approvalFailure(invoiceUuid)
            return
        }
        try {
            val response = pbClient.broadcastTx(baseReq = baseReq, gasEstimate = gasEstimate, mode = ServiceOuterClass.BroadcastMode.BROADCAST_MODE_BLOCK)
                .checkNotNullI { "$logPrefix Null response received from oracle approval transaction" }
                .txResponse
            check(response.code == 0) { "$logPrefix Oracle approval transaction failed. Marking invoice as failed. Error log from Provenance: ${response.rawLog}" }
            logger.info("$logPrefix Oracle approval transaction succeeded. Marking invoice as approved")
            invoiceRepository.update(uuid = invoiceUuid, status = InvoiceStatus.APPROVED)
        } catch (e: Exception) {
            logger.error("Oracle approval failed exceptionally", e)
            approvalFailure(invoiceUuid)
        }
    }

    private fun approvalFailure(invoiceUuid: UUID) {
        invoiceRepository.update(uuid = invoiceUuid, status = InvoiceStatus.APPROVAL_FAILURE)
    }
}
