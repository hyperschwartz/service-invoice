package io.provenance.invoice.services

import arrow.core.Either
import arrow.core.flatMap
import cosmos.auth.v1beta1.Auth.BaseAccount
import cosmos.base.abci.v1beta1.Abci.TxResponse
import cosmos.tx.v1beta1.ServiceOuterClass.BroadcastMode
import cosmos.tx.v1beta1.TxOuterClass.TxBody
import cosmwasm.wasm.v1.Tx.MsgExecuteContract
import io.provenance.client.grpc.BaseReq
import io.provenance.client.grpc.BaseReqSigner
import io.provenance.client.grpc.GasEstimate
import io.provenance.client.grpc.PbClient
import io.provenance.client.protobuf.extensions.getBaseAccount
import io.provenance.invoice.config.provenance.ObjectStore
import io.provenance.invoice.config.provenance.ProvenanceProperties
import io.provenance.invoice.domain.provenancetx.OracleApproval
import io.provenance.invoice.util.extension.checkI
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
    private val objectStore: ObjectStore,
    private val pbClient: PbClient,
    private val provenanceProperties: ProvenanceProperties,
) {
    private companion object : KLogging()

    /**
     * Submits a message to the payable-asset-smart-contract notifying that the oracle account (the address used in
     * the properties of this application) has approved the invoice for payment.  Wraps an errors encountered in an
     * Either that includes a special exception of type OracleApprovalException.  This exception, if encountered, will
     * contain the underlying issue as the cause value.
     */
    fun submitOracleApproval(
        invoiceUuid: UUID,
        logPrefix: String = "ORACLE APPROVAL [$invoiceUuid]:"
    ): Either<OracleApprovalException, TxResponse> {
        logger.info("$logPrefix Marking oracle approval on the chain")
        return getContractAddress(logPrefix)
            .flatMap { contractAddress -> getOracleAccount(logPrefix).map { oracleAccount -> contractAddress to oracleAccount } }
            .flatMap { (contractAddress, oracleAccount) -> getBaseReq(logPrefix, invoiceUuid, contractAddress, oracleAccount) }
            .flatMap { baseReq -> getGasEstimate(logPrefix, baseReq).map { gasEstimate -> baseReq to gasEstimate } }
            .flatMap { (baseReq, gasEstimate) -> broadcastOracleApprovalTx(logPrefix, baseReq, gasEstimate) }
    }

    /**
     * Attempts to fetch the contract address from the nameClient by passing the contract name.  On a failure, wraps the
     * response error in an OracleApprovalException for upstream handling.
     */
    private fun getContractAddress(logPrefix: String): Either<OracleApprovalException, String> = Either
        .catch { pbClient.nameClient.resolve(QueryResolveRequest.newBuilder().setName(provenanceProperties.payablesContractName).build()).address }
        .mapLeftToOracleFailure("$logPrefix Failed to resolve contract by name [${provenanceProperties.payablesContractName}]. Marking invoice with approval failure")

    /**
     * Attempts to fetch the oracle account as a BaseAccount by querying the PbClient with the oracle's Bech32 address.
     * On a failure, wraps the response error in an OracleApprovalException for upstream handling.
     */
    private fun getOracleAccount(logPrefix: String): Either<OracleApprovalException, BaseAccount> = Either
        .catch { pbClient.authClient.getBaseAccount(objectStore.oracleAccountDetail.bech32Address) }
        .mapLeftToOracleFailure("$logPrefix Failed to fetch base account for oracle address [${objectStore.oracleAccountDetail.bech32Address}]. Marking invoice with approval failure")

    /**
     * Attempts to construct a BaseReq from the provided details.  On a failure, wraps the response error in an
     * OracleApprovalException for upstream handling.
     */
    private fun getBaseReq(
        logPrefix: String,
        invoiceUuid: UUID,
        contractAddress: String,
        oracleAccount: BaseAccount
    ): Either<OracleApprovalException, BaseReq> = Either.catch {
        BaseReq(
            signers = BaseReqSigner(
                signer = AccountSigner.fromAccountDetail(objectStore.oracleAccountDetail),
                sequenceOffset = 0,
                account = oracleAccount,
            ).wrapListI(),
            body = TxBody.newBuilder().addMessages(
                MsgExecuteContract.newBuilder()
                    .setMsg(OracleApproval.forUuid(invoiceUuid).toBase64Msg())
                    .setContract(contractAddress)
                    .setSender(objectStore.oracleAccountDetail.bech32Address)
                    .build()
                    .toProtoAnyI()
            ).setMemo("Oracle signature").build(),
            chainId = provenanceProperties.chainId,
            gasAdjustment = 2.0,
        )
    }.mapLeftToOracleFailure("$logPrefix Failed to generate BaseReq for oracle approval transaction")

    /**
     * Attempts to run a tx gas estimation on the provided BaseReq.  On a failure (which indicates that the contract
     * will not accept this transaction due to an error, or the blockchain is having issues) the response error is
     * wrapped in an OracleApprovalException for upstream handling.
     */
    private fun getGasEstimate(logPrefix: String, baseReq: BaseReq): Either<OracleApprovalException, GasEstimate> = Either
        .catch { pbClient.estimateTx(baseReq) }
        .mapLeftToOracleFailure("$logPrefix Failed to estimate gas for oracle approval. Marking invoice with approval failure")

    /**
     * Attempts to broadcast a completed transaction from the provided BaseReq and GasEstimate.  On a failure, the
     * response error is wrapped in an OracleApprovalException for upstream handling.
     */
    private fun broadcastOracleApprovalTx(logPrefix: String, baseReq: BaseReq, gasEstimate: GasEstimate): Either<OracleApprovalException, TxResponse> = Either
        .catch {
            pbClient.broadcastTx(baseReq = baseReq, gasEstimate = gasEstimate, mode = BroadcastMode.BROADCAST_MODE_BLOCK)
                .checkNotNullI { "$logPrefix Null response received from oracle approval transaction" }
                .txResponse
                .checkI(predicate = { it.code == 0 }, lazyMessage = { "$logPrefix Oracle approval transaction failed. Marking invoice as failed" })
        }
        .mapLeftToOracleFailure()

    /**
     * Maps an either derived via an Either.catch{} into a new Either that wraps the resulting Throwable in an
     * OracleApprovalException.  Derives the message for the exception from the wrapped throwable.
     */
    private fun <T: Any> Either<Throwable, T>.mapLeftToOracleFailure(): Either<OracleApprovalException, T> = mapLeft { t ->
        OracleApprovalException(t.message ?: "Oracle approval failed", t)
    }

    /**
     * Maps an either derived via an Either.catch{} into a new Either that wraps the resulting Throwable in an
     * OracleApprovalException.  The original Throwable's message will live within the cause value of the wrapper
     * exception.
     */
    private fun <T: Any> Either<Throwable, T>.mapLeftToOracleFailure(failureMessage: String): Either<OracleApprovalException, T> = mapLeft { t ->
        OracleApprovalException(failureMessage, t)
    }
}

/**
 * A simple custom exception that indicates that the issue encountered is due to the a failure in the oracle approval
 * process for invoices.
 */
class OracleApprovalException(message: String, cause: Throwable) : Exception(message, cause)
