package io.provenance.invoice.domain.entities

import io.provenance.invoice.domain.exceptions.ResourceNotFoundException
import io.provenance.invoice.util.exposed.TextEntityClass
import io.provenance.invoice.util.exposed.TextIdTable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.select

object FailedEventTable : TextIdTable(columnName = "event_hash", name = "failed_event") {
    val processed = bool(name = "processed")
}

open class FailedEventEntityClass(failedEventTable: FailedEventTable): TextEntityClass<FailedEventRecord>(failedEventTable) {
    fun insert(eventHash: String): FailedEventRecord = findById(eventHash)
        ?.also { throw IllegalStateException("Event with hash [$eventHash] has already been stored in the failed events table") }
        .run {
            new(eventHash) {
                this.processed = false
            }
        }

    fun markProcessed(eventHash: String): FailedEventRecord = findById(eventHash)?.apply {
        this.processed = true
    } ?: throw ResourceNotFoundException("Failed to mark failed event [$eventHash] as processed: No record existed in the database")

    fun findAllFailedEventsNotProcessed(): List<String> = FailedEventTable
        .select { FailedEventTable.processed eq false }
        .map { it[FailedEventTable.id].value }
}

class FailedEventRecord(eventHash: EntityID<String>): Entity<String>(eventHash) {
    companion object : FailedEventEntityClass(FailedEventTable)

    // Column setters
    var processed: Boolean by FailedEventTable.processed

    // Derived values
    val eventHash: String = this.id.value
}
