package io.provenance.invoice.util.extension

import arrow.core.Validated

fun <L, R> Validated<L, R>.leftOrNullI(): L? = fold(fe = { it }, fa = { null })
