package io.provenance.invoice.config.logging

enum class MDCKeys(val key: String) {
    REQUEST_STARTED("request_started"),
    REQUEST_UUID("request_uuid"),
}
