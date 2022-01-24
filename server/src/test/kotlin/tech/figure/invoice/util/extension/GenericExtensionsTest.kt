package tech.figure.invoice.util.extension

import org.junit.jupiter.api.Test
import kotlin.test.junit5.JUnit5Asserter.assertEquals
import kotlin.test.junit5.JUnit5Asserter.assertNull

class GenericExtensionsTest {
    @Test
    fun testFlowTo() {
        val a = "String"
        val b = 12
        a.flowTo(b) { string, int ->
            assertEquals(
                expected = a,
                actual = string,
                message = "The flow should maintain the first value",
            )
            assertEquals(
                expected = b,
                actual = int,
                message = "The flow should maintain the second value",
            )
        }
    }

    @Test
    fun testTryOrNull() {
        fun <T: Any> maybeException(value: T, throwException: Boolean): T = value.also {
            if (throwException) {
                throw IllegalStateException("Failed to execute code")
            }
        }
        assertNull(
            actual = tryOrNull { maybeException(10, throwException = true) },
            message = "On an exception, null should be returned",
        )
        assertEquals(
            expected = 10,
            actual = tryOrNull { maybeException(10, throwException = false) },
            message = "When no exception occurs, the returned value should be responded with",
        )
    }
}
