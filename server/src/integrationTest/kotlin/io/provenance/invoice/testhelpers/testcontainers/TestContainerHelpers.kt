package io.provenance.invoice.testhelpers.testcontainers

import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.HttpResponse.response
import io.provenance.invoice.config.app.ConfigurationUtil.DEFAULT_OBJECT_MAPPER
import io.provenance.invoice.util.extension.checkNotNullI

data class SimpleMockResponse(val msg: String)

fun HttpRequest.headerValue(headerName: String): String = headers
    .entries
    .singleOrNull { it.name.value == headerName }
    .checkNotNullI { "No single header with name [$headerName] was present in the request to [${this.path.value}]" }
    .values
    .singleOrNull()
    .checkNotNullI { "Request header [$headerName] did not point to a single value" }
    .value

fun HttpRequest.headerValueOrNull(headerName: String): String? =
    headers.entries.singleOrNull { it.name.value == headerName }?.values?.singleOrNull()?.value

fun HttpResponse.withJsonContentType(): HttpResponse = withHeader("Content-Type", "application/json")

fun <T: Any> HttpResponse.withBodyAsJson(body: T): HttpResponse =
    withBody(DEFAULT_OBJECT_MAPPER.writeValueAsString(body))

fun HttpRequest.tryHandle(fn: (HttpRequest) -> HttpResponse): HttpResponse = try {
    fn(this)
} catch (e: Exception) {
    response().withStatusCode(500)
        .withJsonContentType()
        .withBodyAsJson(SimpleMockResponse(e.message ?: "Internal Server Error"))
}
