package io.provenance.invoice.util.provenance

enum class PayableContractKey(val contractName: String, val isEventKey: Boolean) {
    // All these keys are emitted by the PAYABLE_REGISTERED event
    PAYABLE_REGISTERED("payable_registered", true),
    SCOPE_ID("payable_related_scope_id", false),
    TOTAL_OWED("payable_total_owed", false),
    REGISTERED_DENOM("payable_denom", false),
    ORACLE_FUNDS_KEPT("payable_oracle_funds_kept", false),
    REFUND_AMOUNT("payable_refund_amount", false),

    // All these keys are emitted by the ORACLE_APPROVED event
    ORACLE_APPROVED("payable_oracle_approved", true),

    // All these keys are emitted by the PAYMENT_MADE event
    PAYMENT_MADE("payable_payment_made", true),
    PAYMENT_AMOUNT("payable_amount_paid", false),
    TOTAL_REMAINING("payable_total_remaining", false),
    PAYER("payable_payer", false),
    PAYEE("payable_payee", false),

    // All these keys are globally shared and appear on all payable contract events
    PAYABLE_UUID("payable_uuid", false),
    PAYABLE_TYPE("payable_type", false),
    ORACLE_ADDRESS("payable_oracle_address", false);

    companion object {
        val EVENT_KEYS: List<PayableContractKey> by lazy { values().filter { it.isEventKey } }
        val EVENT_KEYS_CONTRACT_NAMES: List<String> by lazy { EVENT_KEYS.map { it.contractName } }
        val EVENT_KEY_LISTEN_VALUES: List<String> by lazy { EVENT_KEYS_CONTRACT_NAMES.map { "wasm:$it" } }
    }
}
