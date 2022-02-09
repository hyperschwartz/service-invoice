package io.provenance.invoice.test.repository

import helper.assertSingleI
import helper.assertSucceeds
import io.provenance.invoice.repository.FailedEventRepository
import io.provenance.invoice.testhelpers.IntTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertTrue

class FailedEventRepositoryIntTest : IntTestBase() {
    @Autowired lateinit var failedEventRepository: FailedEventRepository

    @Test
    fun testFailedEventInsertAndMark() {
        val fakeHash = "hash-${UUID.randomUUID()}"
        assertSucceeds("A simple insert should succeed") { failedEventRepository.insertEvent(fakeHash) }
        assertSucceeds("Fetching events should succeed") { failedEventRepository.findAllFailedEvents() }
            .assertSingleI("The inserted event should exist in the repository") { failedEvent -> fakeHash == failedEvent }
        assertSucceeds("Marking an event as processed should succeed") { failedEventRepository.markEventProcessed(fakeHash) }
        assertSucceeds("Fetching events when no events are not processed should succeed") { failedEventRepository.findAllFailedEvents() }
            .filter { failedEvent -> failedEvent === fakeHash }
            .also { targetEvents ->
                assertTrue(
                    actual = targetEvents.isEmpty(),
                    message = "The event should no longer be returned by the findAllFailedEvents query becasue it was marked as processed",
                )
            }
    }
}
