package io.provenance.invoice.services

import com.google.protobuf.Any
import io.provenance.invoice.InvoiceProtos.Invoice
import io.provenance.invoice.domain.wallet.WalletDetails
import io.provenance.scope.util.MetadataAddress
import mu.KLogging
import org.springframework.stereotype.Service
import io.provenance.invoice.repository.InvoiceRepository
import io.provenance.invoice.util.enums.InvoiceStatus
import io.provenance.invoice.util.extension.toAsset
import io.provenance.invoice.util.extension.toProtoAny
import io.provenance.invoice.util.extension.toUuid
import io.provenance.invoice.util.extension.parseUuid
import io.provenance.invoice.util.validation.InvoiceValidator
import java.math.BigDecimal
import java.util.UUID

@Service
class InvoiceService(
    private val assetOnboardingService: AssetOnboardingService,
    private val invoiceRepository: InvoiceRepository,
) {
    private companion object : KLogging()

    // TODO: Return onboarding API results for the invoice instead of just validate / insert / return
    fun onboardInvoice(request: OnboardInvoiceRequest): OnboardInvoiceResponse {
        // Ensure first that the invoice is of a valid format
        logger.info("Validating received invoice with uuid [${request.invoice.invoiceUuid.value}] has not yet been boarded")
        InvoiceValidator.validateInvoice(request.invoice)
        logger.info("Verifying invoice with uuid [${request.invoice.invoiceUuid.value}] has not yet been boarded")
        // Verify an invoice with the same uuid does not exist
        check(invoiceRepository.findByUuidOrNull(request.invoice.invoiceUuid.toUuid()) == null) { "Requested invoice uuid [${request.invoice.invoiceUuid.value}] matches previously uploaded invoice" }
        logger.info("Generating onboarding asset from invoice with uuid [${request.invoice.invoiceUuid.value}]")
        val asset = request.invoice.toAsset()
        logger.info("Generating onboarding messages for invoice with uuid [${request.invoice.invoiceUuid.value}]")
        // TODO: Need to store the results from this in the db alongside the invoice to enable retries
        val assetOnboardingResponse = assetOnboardingService.generateInvoiceBoardingTx(asset = asset, walletDetails = request.walletDetails)
        logger.info("Storing successful payload in the database for invoice [${request.invoice.invoiceUuid.value}]")
        val upsertedInvoice = invoiceRepository.insert(
            invoice = request.invoice,
            status = InvoiceStatus.PENDING_STAMP,
            writeScopeRequest = assetOnboardingResponse.writeScopeRequest,
            writeSessionRequest = assetOnboardingResponse.writeSessionRequest,
            writeRecordRequest = assetOnboardingResponse.writeRecordRequest,
        )
        return OnboardInvoiceResponse(
            invoice = upsertedInvoice.invoice,
            payablesContractExecutionDetail = PayablesContractExecutionDetail(
                payableUuid = upsertedInvoice.uuid,
                scopeId = MetadataAddress.forScope(assetOnboardingResponse.writeScopeRequest.scopeUuid.parseUuid()).toString(),
                invoiceTotal = upsertedInvoice.totalOwed,
                invoiceDenom = upsertedInvoice.invoice.paymentDenom,
            ),
            scopeGenerationDetail = ScopeGenerationDetail(
                // Re-package the derived blockchain messages from the asset onboarding response to Any, which is what
                // the frontend expects to receive and send to the blockchain
                writeScopeRequest = assetOnboardingResponse.writeScopeRequest.toProtoAny(),
                writeSessionRequest = assetOnboardingResponse.writeSessionRequest.toProtoAny(),
                writeRecordRequest = assetOnboardingResponse.writeRecordRequest.toProtoAny(),
            ),
        )
    }
}

data class OnboardInvoiceRequest(
    val invoice: Invoice,
    val walletDetails: WalletDetails,
)

data class OnboardInvoiceResponse(
    val invoice: Invoice,
    val payablesContractExecutionDetail: PayablesContractExecutionDetail,
    val scopeGenerationDetail: ScopeGenerationDetail,
)

data class PayablesContractExecutionDetail(
    val payableUuid: UUID,
    val scopeId: String,
    val invoiceDenom: String,
    val invoiceTotal: BigDecimal,
)

data class ScopeGenerationDetail(
    val writeScopeRequest: Any,
    val writeSessionRequest: Any,
    val writeRecordRequest: Any,
)
