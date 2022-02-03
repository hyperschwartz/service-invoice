package io.provenance.invoice.services

import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import cosmos.tx.v1beta1.ServiceOuterClass.BroadcastMode
import cosmos.tx.v1beta1.TxOuterClass
import cosmwasm.wasm.v1.Tx
import io.provenance.client.PbClient
import io.provenance.client.grpc.BaseReq
import io.provenance.client.grpc.GasEstimate
import io.provenance.invoice.AssetProtos.Asset
import io.provenance.invoice.config.provenance.ObjectStore
import io.provenance.invoice.config.provenance.ProvenanceProperties
import io.provenance.invoice.domain.provenancetx.OracleApproval
import io.provenance.invoice.repository.InvoiceRepository
import io.provenance.invoice.util.enums.InvoiceStatus
import io.provenance.invoice.util.eventstream.external.StreamEvent
import io.provenance.invoice.util.extension.checkNotNull
import io.provenance.invoice.util.extension.parseUuid
import io.provenance.invoice.util.extension.toProtoAny
import io.provenance.invoice.util.extension.unpackInvoice
import io.provenance.invoice.util.validation.InvoiceValidator
import io.provenance.name.v1.QueryResolveRequest
import io.provenance.scope.encryption.domain.inputstream.DIMEInputStream
import io.provenance.scope.sdk.extensions.resultHash
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class EventHandlerService(
    private val pbClient: PbClient,
    private val provenanceProperties: ProvenanceProperties,
    private val objectStore: ObjectStore,
    private val invoiceRepository: InvoiceRepository,
) {

    private companion object : KLogging() {
        private const val PAYABLE_REGISTRATION_KEY: String = "PAYABLE_REGISTERED"
        private const val ORACLE_APPROVED_KEY: String = "ORACLE_APPROVED"
        private const val PAYMENT_MADE_KEY: String = "PAYMENT_MADE"

        private val INVOICE_STATUSES_ALLOWED_FOR_ORACLE_APPROVAL = setOf(
            // All newly-boarded invoices end up in this status and should be attempted to be approved
            InvoiceStatus.PENDING_STAMP,
            // Invoices with this status failed to approve via contract execution.  They should be retried
            InvoiceStatus.APPROVAL_FAILURE,
        )
    }

    fun handleEvent(event: StreamEvent) {
        val eventKeys = event.attributes.map { it.key }
        try {
            when {
                PAYABLE_REGISTRATION_KEY in eventKeys -> handleInvoiceRegisteredEvent(event)
                ORACLE_APPROVED_KEY in eventKeys -> handleOracleApprovedEvent(event)
                PAYMENT_MADE_KEY in eventKeys -> handlePaymentMadeEvent(event)
            }
        } catch (e: Exception) {
            logger.error("Failed to process event with hash [${event.txHash}] and type [${event.eventType}] at height [${event.height}]", e)
        }
    }

    private fun handleInvoiceRegisteredEvent(event: StreamEvent) {
        val invoiceUuid = event.attributeValue(PAYABLE_REGISTRATION_KEY)
            .also { logger.info("Handling invoice registration event for invoice uuid [$it]") }
            .parseUuid()
        val invoiceDto = invoiceRepository.findDtoByUuid(invoiceUuid)
        if (invoiceDto.status != InvoiceStatus.PENDING_STAMP) {
        }
        val assetHash = invoiceDto.writeRecordRequest.record.resultHash()
        val getFuture = objectStore.osClient.get(
            hash = assetHash,
            publicKey = objectStore.oracleCredentials.public,
        )
        Futures.addCallback(getFuture, object : FutureCallback<DIMEInputStream> {
            override fun onSuccess(result: DIMEInputStream?) {
                result
                    .checkNotNull { "Null DIMEInputStream received from object store query for invoice [$invoiceUuid]" }
                    .getDecryptedPayload(objectStore.keyRef)
                    .use { signatureStream ->
                        val messageBytes = signatureStream.readAllBytes()
                        val targetInvoice = Asset.parseFrom(messageBytes).unpackInvoice()
                        try {
                            InvoiceValidator.validateInvoice(targetInvoice)
                        } catch (e: Exception) {
                            logger.error("Invoice [$invoiceUuid] failed validation. Marking rejected", e)
                            invoiceRepository.update(uuid = invoiceUuid, status = InvoiceStatus.REJECTED)
                            return
                        }
                        logger.info("Successfully validated invoice from object store [${targetInvoice.invoiceUuid.value}]. Marking oracle approval on the chain")
                        val contractInfo = pbClient.nameClient.resolve(QueryResolveRequest.newBuilder().setName(provenanceProperties.payablesContractName).build())
                        val response = try {
                            pbClient.broadcastTx(
                                baseReq = BaseReq(
                                    signers = emptyList(),
                                    body = TxOuterClass.TxBody.newBuilder().addMessages(
                                        Tx.MsgExecuteContract.newBuilder()
                                            .setMsg(OracleApproval.forUuid(invoiceUuid).toBase64Msg())
                                            .setContract(contractInfo.address)
                                            // TODO: Resolve this somehow
                                            .setSender("tp15e6l9dv8s2rdshjfn34k8a2nju55tr4z42phrt")
                                            .build()
                                            .toProtoAny()
                                    ).setMemo("Oracle signature").build(),
                                    chainId = provenanceProperties.chainId,
                                    gasAdjustment = 2.0,
                                ),
                                gasEstimate = GasEstimate(1905),
                                mode = BroadcastMode.BROADCAST_MODE_BLOCK
                            )
                        } catch (e: Exception) {
                            logger.error("Oracle signing failed", e)
                            invoiceRepository.update(uuid = invoiceUuid, status = InvoiceStatus.APPROVAL_FAILURE)
                            null
                        }
                        if (response != null) {
                            logger.info("Successfully signed as the oracle! Woo! Response: $response")
                            invoiceRepository.update(uuid = invoiceUuid, status = InvoiceStatus.APPROVED)
                        }
                    }
            }
            override fun onFailure(t: Throwable) {
                logger.error("Failed to receive invoice [$invoiceUuid] DIME stream from object store", t)
            }
        }, Executors.newSingleThreadExecutor())
    }

    private fun handleOracleApprovedEvent(event: StreamEvent) {
        logger.info("Handling oracle approved event")
    }

    private fun handlePaymentMadeEvent(event: StreamEvent) {
        logger.info("Handling payment made event")
    }

    private fun StreamEvent.attributeValueOrNull(key: String): String? = attributes.singleOrNull { it.key == key }?.value

    private fun StreamEvent.attributeValue(key: String): String = attributeValueOrNull(key)
        .checkNotNull { "Unable to find stream attribute with key [$key]" }
}
