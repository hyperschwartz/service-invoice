package io.provenance.invoice.domain.entities

import io.provenance.invoice.domain.exceptions.ResourceNotFoundException
import io.provenance.invoice.util.extension.elvis
import io.provenance.invoice.util.offsetDatetime
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime

object WalletNameTable : IdTable<String>(name = "wallet_name") {
    val walletName = varchar("wallet_name", 64).uniqueIndex()
    override val id: Column<EntityID<String>> = walletName.entityId()
    override val primaryKey = PrimaryKey(id)
    val walletAddress = text("wallet_address")
    val createdTime = offsetDatetime("created_time").default(OffsetDateTime.now())
}

class WalletNameRecord(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, WalletNameRecord>(WalletNameTable) {
        private const val MAX_FIND_RESULTS: Int = 10

        fun findByWalletNameOrNull(walletName: String): WalletNameRecord? = transaction {
            find { WalletNameTable.walletName eq walletName }.firstOrNull()
        }

        fun findByWalletName(walletName: String): WalletNameRecord = findByWalletNameOrNull(walletName)
            ?: throw ResourceNotFoundException("Unable to find wallet name record by name [$walletName]")

        fun findByWalletAddressOrNull(walletAddress: String): WalletNameRecord? = transaction {
            find { WalletNameTable.walletAddress eq walletAddress }.firstOrNull()
        }

        fun findByWalletAddress(walletAddress: String): WalletNameRecord = findByWalletAddressOrNull(walletAddress)
            ?: throw ResourceNotFoundException("Unable to find wallet name record by address [$walletAddress]")

        fun insertIfNotPresent(walletAddress: String, walletName: String): WalletNameInsertResponse = transaction {
            findByWalletNameOrNull(walletName)
                ?.let(WalletNameInsertResponse::Existing)
                ?: WalletNameInsertResponse.New(
                    WalletNameTable.insertAndGetId {
                        it[this.walletName] = walletName
                        it[this.walletAddress] = walletAddress
                    }.value
                )
        }

        @Suppress("UNCHECKED_CAST")
        fun findNamesContaining(containsCharacters: String, maxResults: Int? = null): List<WalletNameRecord> = transaction {
            find { WalletNameTable.walletName.lowerCase() like "%${containsCharacters.lowercase()}%" }
                // Don't return an enormous amount of results in case something dumb like 'a' gets passed in
                .limit(maxResults.elvis(MAX_FIND_RESULTS).coerceAtMost(MAX_FIND_RESULTS).coerceAtLeast(1))
                .toList()
        }

        sealed interface WalletNameInsertResponse {
            class New(val recordId: String) : WalletNameInsertResponse
            class Existing(val record: WalletNameRecord) : WalletNameInsertResponse
        }
    }

    val walletName by WalletNameTable.walletName
    val walletAddress by WalletNameTable.walletAddress
    val createdTime by WalletNameTable.createdTime
}
