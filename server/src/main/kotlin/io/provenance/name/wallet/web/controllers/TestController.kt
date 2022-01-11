package io.provenance.name.wallet.web.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.provenance.name.wallet.config.web.Routes

@RestController
@RequestMapping("${Routes.ROUTE_V1}/test", produces = ["application/json"])
class TestController {
    @GetMapping("/response")
    fun getFakeResponse(): String = "You did it. You performed a GET request to an app that doesn't exist. CONGRATUALATION LOL"
}
