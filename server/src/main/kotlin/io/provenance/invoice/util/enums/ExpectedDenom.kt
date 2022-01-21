package io.provenance.invoice.util.enums

enum class ExpectedDenom(val expectedName: String) {
    NHASH(expectedName = "nhash");

    companion object {
        val ALL_EXPECTED_NAMES: Set<String> by lazy { values().map { it.expectedName }.toSet() }
    }
}
