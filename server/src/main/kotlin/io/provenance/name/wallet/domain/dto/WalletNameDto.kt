package io.provenance.name.wallet.domain.dto

import io.provenance.name.wallet.domain.entities.WalletNameRecord
import io.provenance.name.wallet.util.elvis
import java.time.OffsetDateTime

data class WalletNameDto(
    val walletAddress: String,
    val walletName: String,
    val created: OffsetDateTime,
    val isNewRecord: Boolean,
) {
    companion object {
        fun fromInsertResponse(response: WalletNameRecord.Companion.WalletNameInsertResponse): WalletNameDto = response.existingRecord
            .elvis {
                response.newRecordId
                    ?.let { WalletNameRecord.findByWalletAddress(it) }
                    ?: throw IllegalStateException("Database returned null primary key")
            }
            .let { record ->
                WalletNameDto(
                    walletAddress = record.walletAddress.value,
                    walletName = record.walletName,
                    created = record.createdTime,
                    isNewRecord = response.newRecordId != null,
                )
            }

        fun fromRecord(record: WalletNameRecord): WalletNameDto = WalletNameDto(
            walletAddress = record.walletAddress.value,
            walletName = record.walletName,
            created = record.createdTime,
            isNewRecord = false,
        )
    }
}
