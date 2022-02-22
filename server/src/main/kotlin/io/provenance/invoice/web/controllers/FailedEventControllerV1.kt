package io.provenance.invoice.web.controllers

import io.provenance.invoice.config.web.AppRoutes
import io.provenance.invoice.repository.FailedEventRepository
import mu.KLogging
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${AppRoutes.V1}/failed-event", produces = ["application/json"])
class FailedEventControllerV1(private val failedEventRepository: FailedEventRepository) {
    private companion object : KLogging()

    @PostMapping("resolve/{eventHash}")
    fun resolveFailedEvent(@PathVariable eventHash: String) {
        logger.info("Manually resolving failed event [$eventHash]")
        failedEventRepository.markEventProcessed(eventHash)
    }
}
