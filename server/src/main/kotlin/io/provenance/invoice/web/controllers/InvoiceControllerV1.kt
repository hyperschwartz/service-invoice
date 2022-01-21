package io.provenance.invoice.web.controllers

import io.provenance.invoice.config.web.Routes
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Routes.V1}/invoices", produces = ["application/json"])
class InvoiceControllerV1 {
//    // dApp frontend is expected to have a wallet address header in all requests made to the backend
//    @GetMapping("/")
//    fun getName(@RequestHeader(Headers.WALLET_ADDRESS) walletAddress: String): WalletNameDto? =
//        nameService.getNameByAddress(walletAddress)
//
//    @PostMapping("/")
//    fun setName(
//        @RequestHeader(Headers.WALLET_ADDRESS) walletAddress: String,
//        @RequestBody request: SetNameRequest,
//    ): WalletNameDto? = nameService.setName(address = walletAddress, name = request.name)
//
//    @GetMapping("/find/{nameMatcher}")
//    fun findSimilarNames(@PathVariable nameMatcher: String, @RequestParam maxResults: Int?): List<WalletNameDto> =
//        nameService.findNamesContaining(nameMatcher, maxResults)
}

//data class SetNameRequest(val name: String)
