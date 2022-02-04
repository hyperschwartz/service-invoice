package io.provenance.invoice.util.extension

fun <T> T?.elvisI(default: T): T = this ?: default

fun <T> T?.elvisI(lazyDefault: () -> T): T = this ?: lazyDefault()

fun <T: Any?, U: Any?, R> T.flowToI(that: U, transform: (T, U) -> R): R = transform(this, that)

fun <T> tryOrNullI(fn: () -> T): T? = try { fn() } catch (e: Exception) { null }

fun <T> T?.checkNotNullI(lazyMessage: () -> String = { "Expected value to be non-null" }): T {
    check(this != null, lazyMessage)
    return this
}

fun <T> T.checkI(predicate: (T) -> Boolean, lazyMessage: () -> String = { "Check failed" }): T {
    check(predicate.invoke(this), lazyMessage)
    return this
}

fun <T: Any, U: Any> T.ifOrNullI(predicate: (T) -> Boolean, fn: (T) -> U): U? = if (predicate(this)) fn(this) else null

/**
 * Performs an action if the caller is null.  Simple shortcut to avoid annoying .also { if (this == null) { X } } checks
 */
fun <T : Any> T?.ifNullI(action: () -> Unit): T? = this ?: null.also { action() }

fun <T: Any> T.wrapListI(): List<T> = listOf(this)

fun <T: Any> T.wrapSetI(): Set<T> = setOf(this)
