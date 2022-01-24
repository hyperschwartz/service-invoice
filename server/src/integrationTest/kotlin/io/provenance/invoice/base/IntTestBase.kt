package io.provenance.invoice.base

import io.provenance.invoice.Application
import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import java.util.TimeZone

// Enable test containers to start with the app if the spring profile test-containers is included
@ContextConfiguration(initializers = [TestContainersInit::class])
// Enable dependency injection for tests. It is disabled by default
@TestExecutionListeners(DependencyInjectionTestExecutionListener::class)
// Junit cannot alone do the springs
@RunWith(SpringRunner::class)
// Tell the integration tests that the starting point is the same as our production boot app
@SpringBootTest(classes = [Application::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
abstract class IntTestBase {
    @Before
    fun setupIntegrationTests() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }
}
