package io.provenance.invoice.util.extension

import java.time.OffsetDateTime
import java.util.UUID

fun String.toOffsetDateTime(): OffsetDateTime = OffsetDateTime.parse(this)
fun String.toOffsetDateTimeOrNull(): OffsetDateTime? = tryOrNull(::toOffsetDateTime)

fun String.parseUuidOrNull(): UUID? = tryOrNull { UUID.fromString(this) }
fun String.parseUuid(): UUID = parseUuidOrNull().checkNotNull { "Unable to parse value [$this] to a valid UUID" }
