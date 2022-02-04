package io.provenance.invoice.util.enums

enum class PaymentStatus {
    RESTRICTED, // Payments are not available before oracle approval
    REPAY_PERIOD, // Oracle has approved the invoice and payments are now expected
    DELINQUENT, // The due date has been breached and payment is now late
    PAID_ON_TIME, // The final balance was paid on time
    PAID_LATE, // The final balance was paid off after the due date
}
