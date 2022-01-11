package io.provenance.name.wallet.util

fun <T: Any> T?.elvis(default: T): T = this ?: default
fun <T: Any> T?.elvis(lazyDefault: () -> T): T = this ?: lazyDefault()
