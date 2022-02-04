package io.provenance.invoice.util.extension

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

fun LocalDate.isBeforeInclusiveI(that: LocalDate): Boolean = !this.isAfter(that)

fun LocalDate.isAfterInclusiveI(that: LocalDate): Boolean = !this.isBefore(that)

fun LocalDate.daysBetweenI(that: LocalDate): Int = ChronoUnit.DAYS.between(this, that).toInt()

fun OffsetDateTime.isBeforeInclusiveI(that: OffsetDateTime): Boolean = !this.isAfter(that)

fun OffsetDateTime.isAfterInclusiveI(that: OffsetDateTime): Boolean = !this.isBefore(that)

fun OffsetDateTime.daysBetweenI(that: OffsetDateTime): Int = ChronoUnit.DAYS.between(this, that).toInt()
