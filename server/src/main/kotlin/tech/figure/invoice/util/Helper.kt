package tech.figure.invoice.util

import tech.figure.invoice.UtilProtos
import tech.figure.invoice.util.extension.toProtoUuid
import java.util.UUID

fun <T: Any> ifOrNull(condition: Boolean, fn: () -> T): T? = if (condition) fn() else null

fun randomProtoUuid(): UtilProtos.UUID = UUID.randomUUID().toProtoUuid()
