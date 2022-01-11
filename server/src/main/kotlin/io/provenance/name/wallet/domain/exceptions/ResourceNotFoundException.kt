package io.provenance.name.wallet.domain.exceptions

open class ResourceNotFoundException(message: String? = "", cause: Throwable? = null) : RuntimeException(message, cause)
