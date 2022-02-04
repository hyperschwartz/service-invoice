package io.provenance.invoice.calculator

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class PaymentCalc(
    val uuid: UUID,
    val invoiceUuid: UUID,
    val effectiveTime: OffsetDateTime,
    val paymentAmount: BigDecimal,
    val paymentDenom: String,
    val fromAddress: String,
    val owedBeforePayment: BigDecimal,
    val owedAfterPayment: BigDecimal,
)
