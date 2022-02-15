package io.provenance.invoice.util.mock

import io.provenance.invoice.util.enums.ExpectedDenom
import java.math.BigDecimal

object MockInvoiceConstants {
    // The onboarding code validates that this address is a real Bech32 format
    const val DEFAULT_FROM_ADDRESS: String = "tp1hyt8cwsqpgeajjxy92098tn27uyuqp25d30rdv"
    const val DEFAULT_TO_ADDRESS: String = "receiver"
    val DEFAULT_PAYMENT_DENOM: String = ExpectedDenom.NHASH.expectedName
    const val DEFAULT_LINE_NAME: String = "Money Request"
    const val DEFAULT_LINE_DESCRIPTION: String = "SHOW ME THE MONEY"
    const val DEFAULT_LINE_QUANTITY: Int = 1
    val DEFAULT_LINE_PRICE: BigDecimal = "100".toBigDecimal()
}
