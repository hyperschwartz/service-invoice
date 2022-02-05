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
        expected = expected?.testRoundingI(scale),
        actual = actual?.testRoundingI(scale),
        message = message,
    )
}

fun assertZeroBD(actual: BigDecimal?, message: String? = null) {
    assertEqualsBD(
        expected = BigDecimal.ZERO,
        actual = actual,
        message = message,
    )
}

