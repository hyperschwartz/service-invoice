package tech.figure.invoice.web.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tech.figure.invoice.config.web.AppRoutes
import tech.figure.invoice.util.enums.ExpectedDenom

@RestController
@RequestMapping("${AppRoutes.V1}/denom", produces = ["application/json"])
class DenomControllerV1 {
    @GetMapping("/all")
    fun getAllDenominations(): Set<String> = ExpectedDenom.ALL_EXPECTED_NAMES
}
