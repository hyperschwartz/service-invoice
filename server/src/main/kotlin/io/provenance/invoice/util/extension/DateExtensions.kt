package io.provenance.invoice.util.extension

import java.time.LocalDate

fun LocalDate.isBeforeInclusive(that: LocalDate): Boolean = !this.isAfter(that)

fun LocalDate.isAfterInclusive(that: LocalDate): Boolean = !this.isBefore(that)
