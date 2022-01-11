package io.provenance.name.wallet.web.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.provenance.name.wallet.config.web.Routes
import io.provenance.name.wallet.domain.dto.WalletNameDto
import io.provenance.name.wallet.domain.entities.WalletNameRecord
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

// TODO: Remove all of this trash
@RestController
@RequestMapping("${Routes.V1}/test", produces = ["application/json"])
class TestController {
    @GetMapping("/response")
    fun getFakeResponse(): String = "You did it. You performed a GET request to an app that doesn't exist. CONGRATUALATION LOL"

    @PostMapping("/insert-wallet-name")
    fun insertWalletName(@RequestParam walletAddress: String, @RequestParam walletName: String): WalletNameDto = WalletNameRecord
        .insertIfNotPresent(walletAddress, walletName)
        .let(WalletNameDto::fromInsertResponse)

    @GetMapping("/wallet-name/by-address")
    fun getWalletNameByAddress(@RequestParam walletAddress: String): WalletNameDto? = WalletNameRecord
        .findByWalletAddressOrNull(walletAddress)
        ?.let(WalletNameDto::fromRecord)

    @GetMapping("/wallet-name/by-name")
    fun getWalletNameByName(@RequestParam walletName: String): WalletNameDto? = WalletNameRecord
        .findByWalletNameOrNull(walletName)
        ?.let(WalletNameDto::fromRecord)

    @GetMapping("/wallet-name/all")
    fun getAllWalletNames(): List<WalletNameDto> = WalletNameRecord.findAll().map(WalletNameDto::fromRecord)
}
