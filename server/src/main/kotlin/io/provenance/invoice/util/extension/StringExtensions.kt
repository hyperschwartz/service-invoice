package io.provenance.invoice.util.extension

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

fun String.toOffsetDateTimeI(): OffsetDateTime = OffsetDateTime.parse(this)
fun String.toOffsetDateTimeOrNullI(): OffsetDateTime? = tryOrNullI(::toOffsetDateTimeI)

fun String.toUuidOrNullI(): UUID? = tryOrNullI { UUID.fromString(this) }
fun String.toUuidI(): UUID = toUuidOrNullI().checkNotNullI { "Unable to parse value [$this] to a valid UUID" }

fun String.toLocalDateI(): LocalDate = LocalDate.parse(this)
fun String.toLocalDateOrNullI(): LocalDate? = tryOrNullI { toLocalDateI() }

/**
 * Simple shortcut to ignore case when comparing Strings instead of using equals with a flag.
 */
fun String.equalsIgnoreCaseI(that: String): Boolean = this.equals(that, ignoreCase = true)

/**
 * The standard Kotlin toBoolean() does some funky logic that can cause some incorrect values to be parsed.
 * The Kotlin check is "true".equalsIgnoreCase(value), which will cause anything except case-insensitive variations of "true" to return false.
 * This is not ideal.  We want "true" variations to be true, and "false" variations to be false.  Anything else should be returned as null,
 * and not a false-negative.
 * Additional note: This function also behaves differently than Kotlin 1.5's toBooleanStrictOrNull.
 * toBooleanStrictOrNull will not use "ignore case" and return null for values like "TRUE" or "FALSE"
 */
fun String?.toBooleanIgnoreCaseI(): Boolean? = this?.let { stringNotNull ->
    when {
        stringNotNull.equalsIgnoreCaseI("true") -> true
        stringNotNull.equalsIgnoreCaseI("false") -> false
        else -> null
    }
}
