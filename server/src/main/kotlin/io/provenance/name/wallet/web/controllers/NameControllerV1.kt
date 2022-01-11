package io.provenance.name.wallet.web.controllers

import io.provenance.name.wallet.config.web.Headers
import io.provenance.name.wallet.config.web.Routes
import io.provenance.name.wallet.domain.dto.WalletNameDto
import io.provenance.name.wallet.services.NameService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Routes.V1}/names", produces = ["application/json"])
class NameControllerV1(private val nameService: NameService) {
    // dApp frontend is expected to have a wallet address header in all requests made to the backend
    @GetMapping("/")
    fun getName(@RequestHeader(Headers.WALLET_ADDRESS) walletAddress: String): WalletNameDto? =
        nameService.getNameByAddress(walletAddress)

    @PostMapping("/")
    fun setName(
        @RequestHeader(Headers.WALLET_ADDRESS) walletAddress: String,
        @RequestBody request: SetNameRequest,
    ): WalletNameDto? = nameService.setName(address = walletAddress, name = request.name)

    @GetMapping("/find/{nameMatcher}")
    fun findSimilarNames(@PathVariable nameMatcher: String, @RequestParam maxResults: Int?): List<WalletNameDto> =
        nameService.findNamesContaining(nameMatcher, maxResults)
}

data class SetNameRequest(val name: String)
