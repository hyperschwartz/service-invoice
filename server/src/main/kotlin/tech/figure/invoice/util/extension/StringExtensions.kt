package tech.figure.invoice.util.extension

import java.time.OffsetDateTime

fun String.toOffsetDateTime(): OffsetDateTime = OffsetDateTime.parse(this)
fun String.toOffsetDateTimeOrNull(): OffsetDateTime? = tryOrNull(::toOffsetDateTime)
