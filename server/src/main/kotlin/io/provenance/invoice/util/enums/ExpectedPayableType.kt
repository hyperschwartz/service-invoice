package io.provenance.invoice.util.enums

// TODO: Move this to some global library if payables become an important thing
enum class ExpectedPayableType(val contractName: String) {
    INVOICE("invoice"),
}
