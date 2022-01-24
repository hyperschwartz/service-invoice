package tech.figure.invoice.util.extension

fun <T: Any> T?.elvis(default: T): T = this ?: default

fun <T: Any> T?.elvis(lazyDefault: () -> T): T = this ?: lazyDefault()

fun <T: Any?, U: Any?, R> T.flowTo(that: U, transform: (T, U) -> R): R = transform(this, that)

fun <T> tryOrNull(fn: () -> T): T? = try { fn() } catch (e: Exception) { null }

fun <T> T?.checkNotNull(lazyMessage: () -> String = { "Expected value to be non-null" }): T {
    check(this != null, lazyMessage)
    return this
}

fun <T> T.check(predicate: (T) -> Boolean, lazyMessage: () -> String = { "Check failed" }): T {
    check(predicate.invoke(this), lazyMessage)
    return this
}

fun <T: Any, U: Any> T.ifOrNull(predicate: (T) -> Boolean, fn: (T) -> U): U? = if (predicate(this)) fn(this) else null
