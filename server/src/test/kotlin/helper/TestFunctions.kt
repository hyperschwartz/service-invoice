package helper

import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.fail

fun <T> assertSucceeds(message: String = "Block failed to execute without exception", block: () -> T): T = try {
    block()
} catch (e: Exception) {
    fail(message, e)
}

fun assertEqualsBD(expected: BigDecimal?, actual: BigDecimal?, scale: Int = 5, message: String? = null) {
    assertEquals(
        expected = expected?.testRounding(scale),
        actual = actual?.testRounding(scale),
        message = message,
    )
}

