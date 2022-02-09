package io.provenance.invoice.repository

import io.provenance.invoice.domain.entities.FailedEventRecord
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Repository

@Repository
class FailedEventRepository {
    fun insertEvent(eventHash: String): FailedEventRecord = transaction { FailedEventRecord.insert(eventHash) }

    fun markEventProcessed(eventHash: String): FailedEventRecord = transaction { FailedEventRecord.markProcessed(eventHash) }

    fun findAllFailedEvents(): List<String> = transaction { FailedEventRecord.findAllFailedEventsNotProcessed() }
}
