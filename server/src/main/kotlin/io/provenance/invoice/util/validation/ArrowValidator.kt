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

/**
 * This class is designed to be inherited from by a class with a custom validation definition, E.  The inheritor will
 * define a group of Arrow Validated<E, *> values and group them into the protected list by invoking .bindValidation()
 * on them.
 *
 * After the validations have been declared and generated (this can occur as instance variables, in an init block, or
 * dynamically via invocations from an external source), the primary functionality is to invoke
 * .generateValidationReport(), which will aggregate all errors that occurred throughout validation into a comprehensive
 * error message.
 *
 * @param E The type of validation error that all internal errors must conform to.  There is no reason that this value
 *          cannot simply be objects that directly extend from ArrowValidationError, but subclassing is recommended for
 *          a concise approach that cannot intermingle with other implementations.
 */
open class ArrowValidator <E: ArrowValidationError> {
    /**
     * Overrideable to allow implementors to display their own, more detailed string, when a validation report is
     * generated.
     */
    open fun getFailurePrefix(): String = "Validation failed"

    // Inner validation collection.  Automatically appended to when bindValidation is called on declared Validated
    // instances.
    protected val validations: MutableList<Validated<E, *>> = mutableListOf()

    /**
     * A type-specific extension function that allows the inheriting class to functionally declare Validated instances
     * and append them to the validations list with ease.
     */
    fun <U: Any, T: Validated<E, U>> T.bindValidation(): T = this.also { validations.add(this) }

    /**
     * A helper function that allows validations from a different validator that still conforms to type E to be appended
     * to the validations list.  Useful for nested classes.
     */
    fun <T: ArrowValidator<E>> bindValidationsFrom(validator: T) {
        validations.addAll(validator.validations)
    }

    /**
     * After all validations are generated, use this report to pull the validation errors by target or as a mass string.
     */
    fun generateValidationReport(): ArrowValidationReport<E> = ArrowValidationReport(
        failures = validations.mapNotNull { it.leftOrNullI() },
        failurePrefix = getFailurePrefix(),
    )
}

class ArrowValidationException(message: String, cause: Throwable? = null): Exception(message, cause)
