package io.provenance.invoice.config.web

import io.provenance.invoice.config.logging.MDCKeys
import io.provenance.invoice.util.extension.toOffsetDateTimeOrNullI
import mu.KLogging
import org.slf4j.MDC
import org.springframework.web.servlet.HandlerInterceptor
import java.lang.Exception
import java.time.OffsetDateTime
import java.util.UUID
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class WebRequestLoggingInterceptor : HandlerInterceptor {
    private companion object : KLogging() {
        private val LOGGED_HEADERS: Set<String> = setOf(
            "x-address",
        )
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        // preHandle gets called multiple times - only tag once
        if (MDC.get(MDCKeys.REQUEST_STARTED.key) != null) {
            return true
        }
        // Tag each request with a UUID for easy tracing in debugging
        MDC.put(MDCKeys.REQUEST_UUID.key, UUID.randomUUID().toString())
        // Take note of when each request started to trace long-running requests back to the source
        MDC.put(MDCKeys.REQUEST_STARTED.key, OffsetDateTime.now().toString())
        // Append all relevant headers to MDC when found
        request.headerNames
            .toList()
            .filter { it.lowercase() in LOGGED_HEADERS }
            .forEach { header -> MDC.put(header, request.getHeaders(header).toList().joinToString()) }
        logger.info("START request [${request.requestURL}]")
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        // If MDC request time exists on an executed request, log the amount of time the route took
        MDC.get(MDCKeys.REQUEST_STARTED.key)?.toOffsetDateTimeOrNullI()?.toInstant()?.toEpochMilli()?.let { startTimeMillis ->
            logger.info("END request [${request.requestURL}] | Took ${OffsetDateTime.now().toInstant().toEpochMilli() - startTimeMillis}ms")
        }
    }
}
