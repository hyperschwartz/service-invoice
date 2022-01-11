package io.provenance.name.wallet.services

import io.provenance.name.wallet.domain.dto.WalletNameDto
import io.provenance.name.wallet.domain.entities.WalletNameRecord
import org.springframework.stereotype.Service

@Service
class NameService {
    fun getNameByAddress(address: String): WalletNameDto? = WalletNameRecord.findByWalletAddressOrNull(address)
        ?.let(WalletNameDto::fromRecord)

    fun setName(address: String, name: String): WalletNameDto = WalletNameRecord.insertIfNotPresent(
        walletAddress = address,
        walletName = name,
    ).let(WalletNameDto::fromInsertResponse)

    fun findNamesContaining(matcher: String, maxResults: Int? = null): List<WalletNameDto> = WalletNameRecord.findNamesContaining(
        containsCharacters = matcher,
        maxResults = maxResults,
    ).map(WalletNameDto::fromRecord)
}
