package io.provenance.invoice.web.controllers

import io.provenance.invoice.config.web.AppRoutes
import io.provenance.invoice.util.enums.ExpectedDenom
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${AppRoutes.V1}/denom", produces = ["application/json"])
class DenomControllerV1 {
    @GetMapping("/all")
    fun getAllDenominations(): Set<String> = ExpectedDenom.ALL_EXPECTED_NAMES
}
