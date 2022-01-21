package io.provenance.invoice.config.web

import io.provenance.invoice.domain.exceptions.ResourceNotFoundException
import mu.KLogging
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import javax.servlet.http.HttpServletRequest

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class GlobalControllerAdvice : ResponseEntityExceptionHandler() {
    private companion object : KLogging()

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFoundException(
        exception: ResourceNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<Any> = handle404(exception, request)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        exception: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<Any> = handle404(exception, request)

    private fun <T: Exception> handle404(exception: T, request: HttpServletRequest): ResponseEntity<Any> {
        logger.warn("404 Encountered on [${request.requestURI}] with error [${exception::class.qualifiedName}: ${exception.message}]")
        return ResponseEntity.notFound().build()
    }
}
