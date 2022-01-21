package io.provenance.invoice.web

import io.provenance.invoice.domain.exceptions.ResourceNotFoundException
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component

/**
 * Any methods with the RequestMapping (or subtype) annotations will be monitored.
 * If the return is null, throw a ResourceNotFoundException which is caught by the GlobalErrorHandler and produces a 404 response
 * If the method return type is void, do nothing
 *
 * Credit goes to Mike Emery for creating this glorious thing
 */
@Aspect
@Component
class NullReturnAspect {
    @AfterReturning(
        pointcut = """
            !execution(void *(..))
            &&
            (execution(@org.springframework.web.bind.annotation.RequestMapping * *.*(..))
                || execution(@org.springframework.web.bind.annotation.GetMapping * *.*(..))
                || execution(@org.springframework.web.bind.annotation.PutMapping * *.*(..))
                || execution(@org.springframework.web.bind.annotation.PostMapping * *.*(..))
                || execution(@org.springframework.web.bind.annotation.DeleteMapping * *.*(..)))
                || execution(@org.springframework.web.bind.annotation.PatchMapping * *.*(..))
            """,
        returning = "result"
    )
    fun intercept(result: Any?) {
        result ?: throw ResourceNotFoundException()
    }
}
