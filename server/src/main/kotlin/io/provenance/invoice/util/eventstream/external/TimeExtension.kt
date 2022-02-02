package io.provenance.invoice.util.eventstream.external

import mu.KotlinLogging

private val timerLogger = KotlinLogging.logger("io.provenance.invoice.util.eventstream.timer")

fun <R> timed(item: String, fn: () -> R): R {
    val start = System.currentTimeMillis()
    try {
        return fn()
    } finally {
        val end = System.currentTimeMillis()
        val elapsed = end - start
        timerLogger.info("timed($item) => elapsed:${elapsed}ms  (start:$start end:$end)")
    }
}
