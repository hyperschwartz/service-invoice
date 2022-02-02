package io.provenance.invoice.util.eventstream.external

open class TransactionQueryException(message: String) : RuntimeException(message)

class TransactionNotFoundException(message: String) : TransactionQueryException(message)
