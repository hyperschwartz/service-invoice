package io.provenance.invoice.domain.exceptions

open class ResourceNotFoundException(message: String? = "", cause: Throwable? = null) : RuntimeException(message, cause)
