package io.provenance.name.wallet.domain.entities

import io.provenance.name.wallet.domain.exceptions.ResourceNotFoundException
import io.provenance.name.wallet.util.ifOrNull
import io.provenance.name.wallet.util.offsetDatetime
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime

object WalletNameTable : IdTable<String>(name = "wallet_name") {
    override val id: Column<EntityID<String>> = varchar("wallet_address", 64).entityId()
    override val primaryKey = PrimaryKey(id)
    val walletName = varchar("wallet_name", 64)
    val createdTime = offsetDatetime("created_time").default(OffsetDateTime.now())
}

class WalletNameRecord(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, WalletNameRecord>(WalletNameTable) {
        fun findByWalletAddressOrNull(walletAddress: String): WalletNameRecord? = transaction {
            find { WalletNameTable.id eq walletAddress }.firstOrNull()
        }

        fun findByWalletAddress(walletAddress: String): WalletNameRecord = findByWalletAddressOrNull(walletAddress)
            ?: throw ResourceNotFoundException("Unable to find wallet name record by address [$walletAddress]")

        fun findByWalletNameOrNull(walletName: String): WalletNameRecord? = transaction {
            find { WalletNameTable.walletName eq walletName }.firstOrNull()
        }

        fun findByWalletName(walletName: String): WalletNameRecord = findByWalletNameOrNull(walletName)
            ?: throw ResourceNotFoundException("Unable to find wallet name record by name [$walletName]")

        fun findByWalletAddressOrNameOrNull(walletAddress: String, walletName: String): WalletNameRecord? = transaction {
            find { (WalletNameTable.id eq walletAddress) or (WalletNameTable.walletName eq walletName) }
                .firstOrNull()
        }

        fun insertIfNotPresent(walletAddress: String, walletName: String): WalletNameInsertResponse = transaction {
            findByWalletAddressOrNameOrNull(walletAddress, walletName).let { existingRecordOrNull ->
                WalletNameInsertResponse(
                    existingRecord = existingRecordOrNull,
                    newRecordId = ifOrNull (existingRecordOrNull == null) {
                        WalletNameTable.insertAndGetId {
                            it[this.id] = walletAddress
                            it[this.walletName] = walletName
                        }.value
                    }
                )
            }
        }

        // TODO: Definitely remove this garbage
        fun findAll(): List<WalletNameRecord> = transaction { find { WalletNameTable.id.isNotNull() }.toList() }

        data class WalletNameInsertResponse(
            val existingRecord: WalletNameRecord?,
            val newRecordId: String?,
        )
    }

    val walletAddress by WalletNameTable.id
    val walletName by WalletNameTable.walletName
    val createdTime by WalletNameTable.createdTime
}
