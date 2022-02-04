package io.provenance.invoice.util

import io.provenance.invoice.UtilProtos
import io.provenance.invoice.util.extension.toProtoUuidI
import java.util.UUID

fun <T: Any> ifOrNullI(condition: Boolean, fn: () -> T): T? = if (condition) fn() else null

fun randomProtoUuidI(): UtilProtos.UUID = UUID.randomUUID().toProtoUuidI()
