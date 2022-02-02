package io.provenance.invoice.services

import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import io.provenance.invoice.AssetProtos.Asset
import io.provenance.invoice.config.provenance.ObjectStore
import io.provenance.invoice.repository.InvoiceRepository
import io.provenance.invoice.util.eventstream.external.StreamEvent
import io.provenance.invoice.util.extension.checkNotNull
import io.provenance.invoice.util.extension.parseUuid
import io.provenance.invoice.util.extension.unpackInvoice
import io.provenance.invoice.util.validation.InvoiceValidator
import io.provenance.scope.encryption.domain.inputstream.DIMEInputStream
import io.provenance.scope.sdk.extensions.resultHash
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class EventHandlerService(
    private val objectStore: ObjectStore,
    private val invoiceRepository: InvoiceRepository,
) {

    private companion object : KLogging() {
        private const val PAYABLE_REGISTRATION_KEY: String = "PAYABLE_REGISTERED"
        private const val ORACLE_APPROVED_KEY: String = "ORACLE_APPROVED"
        private const val PAYMENT_MADE_KEY: String = "PAYMENT_MADE"
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
                        InvoiceValidator.validateInvoice(targetInvoice)
                        logger.info("Successfully validated invoice from object store [${targetInvoice.invoiceUuid.value}]")
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
