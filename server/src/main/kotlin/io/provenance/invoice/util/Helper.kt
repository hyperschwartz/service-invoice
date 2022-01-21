package io.provenance.invoice.util

import io.provenance.invoice.UtilProtos
import io.provenance.invoice.util.extension.toProtoUuid
import java.util.UUID

fun <T: Any> ifOrNull(condition: Boolean, fn: () -> T): T? = if (condition) fn() else null

fun randomProtoUuid(): UtilProtos.UUID = UUID.randomUUID().toProtoUuid()
