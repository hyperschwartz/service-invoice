package io.provenance.invoice.services

import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import io.provenance.invoice.AssetProtos.Asset
import io.provenance.invoice.config.provenance.ObjectStore
import io.provenance.invoice.factory.InvoiceCalcFactory
import io.provenance.invoice.repository.InvoiceRepository
import io.provenance.invoice.repository.PaymentRepository
import io.provenance.invoice.util.enums.ExpectedPayableType
import io.provenance.invoice.util.enums.InvoiceStatus
import io.provenance.invoice.util.eventstream.external.StreamEvent
import io.provenance.invoice.util.extension.attributeValueI
import io.provenance.invoice.util.extension.checkNotNullI
import io.provenance.invoice.util.extension.unpackInvoiceI
import io.provenance.invoice.util.provenance.PayableContractKey
import io.provenance.invoice.util.validation.InvoiceValidator
import io.provenance.scope.encryption.domain.inputstream.DIMEInputStream
import io.provenance.scope.sdk.extensions.resultHash
import mu.KLogging
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.Executors

@Service
class EventHandlerService(
    private val invoiceCalcFactory: InvoiceCalcFactory,
    private val objectStore: ObjectStore,
    private val invoiceRepository: InvoiceRepository,
    private val paymentRepository: PaymentRepository,
    private val provenanceQueryService: ProvenanceQueryService,
) {

    private companion object : KLogging() {
        private val INVOICE_STATUSES_ALLOWED_FOR_ORACLE_APPROVAL = setOf(
            // All newly-boarded invoices end up in this status and should be attempted to be approved
            InvoiceStatus.PENDING_STAMP,
            // Invoices with this status failed to approve via contract execution.  They should be retried
            InvoiceStatus.APPROVAL_FAILURE,
        )

        private val INVOICE_STATUSES_ALLOWED_FOR_PAYMENT = setOf(
            // Payments should only be allowed for oracle-approved invoices
            InvoiceStatus.APPROVED,
        )

        private val HANDLER_THREAD_POOL = Executors.newFixedThreadPool(10)
    }

    fun handleEvent(event: StreamEvent) {
        try {
            // Payable type should be emitted by all events
            val payableType = event.attributeValueI<String>(PayableContractKey.PAYABLE_TYPE)
            if (payableType != ExpectedPayableType.INVOICE.contractName) {
                logger.info("Ignoring event [${event.txHash}] for non-invoice payable with type [$payableType]")
                return
            }
            val incomingEvent = IncomingInvoiceEvent(
                streamEvent = event,
                invoiceUuid = event.attributeValueI(PayableContractKey.PAYABLE_UUID)
            )
            val eventKeys = event.attributes.map { it.key }
            when {
                PayableContractKey.PAYABLE_REGISTERED.contractName in eventKeys -> handleInvoiceRegisteredEvent(incomingEvent)
                PayableContractKey.ORACLE_APPROVED.contractName in eventKeys -> handleOracleApprovedEvent(incomingEvent)
                PayableContractKey.PAYMENT_MADE.contractName in eventKeys -> handlePaymentMadeEvent(incomingEvent)
            }
        } catch (e: Exception) {
            logger.error("Failed to process event with hash [${event.txHash}] and type [${event.eventType}] at height [${event.height}]", e)
        }
    }

    fun handleInvoiceRegisteredEvent(event: IncomingInvoiceEvent) {
        val logPrefix = "INVOICE REGISTRATION [${event.invoiceUuid}]:"
        logger.info("$logPrefix: Handling event")
        val invoiceDto = invoiceRepository.findDtoByUuid(event.invoiceUuid)
        if (invoiceDto.status !in INVOICE_STATUSES_ALLOWED_FOR_ORACLE_APPROVAL) {
            logger.info("$logPrefix Skipping for finalized invoice with status [${invoiceDto.status.name}]")
            return
        }
        val assetHash = invoiceDto.writeRecordRequest.record.resultHash()
        val getFuture = objectStore.osClient.get(
            hash = assetHash,
            publicKey = objectStore.oracleAccountDetail.publicKey,
        )
        Futures.addCallback(getFuture, object : FutureCallback<DIMEInputStream> {
            override fun onSuccess(result: DIMEInputStream?) {
                result
                    .checkNotNullI { "$logPrefix Null DIMEInputStream received from object store query" }
                    .getDecryptedPayload(objectStore.oracleAccountDetail.keyRef)
                    .use { signatureStream ->
                        val messageBytes = signatureStream.readAllBytes()
                        val targetInvoice = Asset.parseFrom(messageBytes).unpackInvoiceI()
                        try {
                            InvoiceValidator.validateInvoiceForApproval(
                                invoiceDto = invoiceDto,
                                objectStoreInvoice = targetInvoice,
                                eventScopeId = event.streamEvent.attributeValueI(PayableContractKey.SCOPE_ID),
                                eventTotalOwed = event.streamEvent.attributeValueI(PayableContractKey.TOTAL_OWED),
                                eventInvoiceDenom = event.streamEvent.attributeValueI(PayableContractKey.REGISTERED_DENOM),
                            )
                        } catch (e: Exception) {
                            logger.error("$logPrefix Failed validation. Marking rejected", e)
                            invoiceRepository.update(uuid = event.invoiceUuid, status = InvoiceStatus.REJECTED)
                            return
                        }
                        logger.info("$logPrefix Successfully validated invoice from object store")
                        provenanceQueryService.submitOracleApproval(event.invoiceUuid, logPrefix)
                    }
            }
            override fun onFailure(t: Throwable) {
                logger.error("$logPrefix Failed to receive DIME stream from object store", t)
            }
        }, HANDLER_THREAD_POOL)
    }

    fun handleOracleApprovedEvent(event: IncomingInvoiceEvent) {
        logger.info("Handling oracle approved event")
    }

    fun handlePaymentMadeEvent(event: IncomingInvoiceEvent) {
        // Derive the payment uuid from the incoming TX hash.  This will prevent duplicate payments from being
        // stored from re-running the same event
        val paymentUuid = UUID.nameUUIDFromBytes(event.streamEvent.txHash.toByteArray())
        val logPrefix = "PAYMENT MADE [Invoice ${event.invoiceUuid} | Payment $paymentUuid]:"
        logger.info("$logPrefix Handling payment made event")
        val calc = invoiceCalcFactory.generate(event.invoiceUuid)
        if (calc.payments.any { it.uuid == paymentUuid }) {
            logger.info("$logPrefix Skipping duplicate payment from event hash [${event.streamEvent.txHash}]")
            return
        }
        if (calc.invoiceStatus !in INVOICE_STATUSES_ALLOWED_FOR_PAYMENT) {
            logger.error("$logPrefix Payment received for invoice with status [${calc.invoiceStatus}]. This is a bug. Please investigate")
        }
        if (calc.isPaidOff) {
            logger.error("$logPrefix Payment received for paid off invoice [${calc.uuid}]")
        }
        logger.info("$logPrefix Storing new received payment in the db")
        paymentRepository.insert(
            paymentUuid = paymentUuid,
            invoiceUuid = event.invoiceUuid,
            paymentTime = OffsetDateTime.now(),
            fromAddress = event.streamEvent.attributeValueI(PayableContractKey.PAYER),
            toAddress = event.streamEvent.attributeValueI(PayableContractKey.PAYEE),
            paymentAmount = event.streamEvent.attributeValueI(PayableContractKey.PAYMENT_AMOUNT),
        )
    }


}

data class IncomingInvoiceEvent(val streamEvent: StreamEvent, val invoiceUuid: UUID)
