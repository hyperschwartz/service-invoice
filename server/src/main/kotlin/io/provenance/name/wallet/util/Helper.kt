package io.provenance.name.wallet.util

fun <T: Any> ifOrNull(condition: Boolean, fn: () -> T): T? = if (condition) fn() else null

fun <T: Any, U: Any> T.ifOrNull(predicate: (T) -> Boolean, fn: (T) -> U): U? = if (predicate(this)) fn(this) else null
