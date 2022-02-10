package io.provenance.invoice.util.validation

import arrow.core.Validated
import io.provenance.invoice.util.extension.leftOrNullI
import kotlin.reflect.KClass

/**
 * Standard wrapper interface for errors bound during the flow of creating an ArrowValidator
 * @field fieldName The field being validated.
 * @field errorMessage The human readable description of the error that occurred.
 */
interface ArrowValidationError {
    val fieldName: String
    val errorMessage: String
}

/**
 * A report that aggregates all failed validiations in an ArrowValidator.
 * @param failures All failures that occurred when validating a given ArrowValidator.
 * @param failurePrefix The prefix to be appended to the failure message.  Can be defined by overriding the
 *                      ArrowValidator's getFailurePrefix function.
 */
data class ArrowValidationReport<E : ArrowValidationError>(
    val failures: List<E>,
    private val failurePrefix: String,
) {
    val failureMap: Map<KClass<*>, ArrowValidationError> = failures.associateBy { it::class }

    val failed = failures.isNotEmpty()

    inline fun <reified T: Any> hadError(): Boolean = failureMap[T::class] != null

    fun getFailureMessage(separator: String = System.lineSeparator()): String =
        "$failurePrefix:$separator${failures.joinToString(separator) { failure -> "[${failure.fieldName}]: ${failure.errorMessage}"}}"

    fun throwFailures(separator: String = System.lineSeparator()) {
        if (failed) {
            throw ArrowValidationException(getFailureMessage(separator))
        }
    }
}

open class ArrowValidator <E: ArrowValidationError> {
    open fun getFailurePrefix(): String = "Validation failed"

    protected val validations: MutableList<Validated<E, *>> = mutableListOf()

    fun <U: Any, T: Validated<E, U>> T.bindValidation(): T = this.also { validations.add(this) }

    fun <T: ArrowValidator<E>> bindValidationsFrom(validator: T) {
        validations.addAll(validator.validations)
    }

    fun generateValidationReport(): ArrowValidationReport<E> = ArrowValidationReport(
        failures = validations.mapNotNull { it.leftOrNullI() },
        failurePrefix = getFailurePrefix(),
    )
}

class ArrowValidationException(message: String, cause: Throwable? = null): Exception(message, cause)
