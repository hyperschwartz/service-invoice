package io.provenance.name.wallet.domain.dto

import io.provenance.name.wallet.domain.entities.WalletNameRecord
import io.provenance.name.wallet.domain.entities.WalletNameRecord.Companion.WalletNameInsertResponse
import io.provenance.name.wallet.domain.entities.WalletNameRecord.Companion.WalletNameInsertResponse.Existing
import io.provenance.name.wallet.domain.entities.WalletNameRecord.Companion.WalletNameInsertResponse.New
import java.time.OffsetDateTime

data class WalletNameDto(
    val walletName: String,
    val walletAddress: String,
    val created: OffsetDateTime,
    val isNewRecord: Boolean,
) {
    companion object {
        fun fromInsertResponse(response: WalletNameInsertResponse): WalletNameDto = when(response) {
            is New -> WalletNameRecord.findByWalletName(response.recordId)
            is Existing -> response.record
        }.let(::fromRecord)

        fun fromRecord(record: WalletNameRecord): WalletNameDto = WalletNameDto(
            walletAddress = record.walletAddress,
            walletName = record.walletName,
            created = record.createdTime,
            isNewRecord = false,
        )
    }
}
