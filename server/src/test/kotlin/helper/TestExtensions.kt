package helper

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.fail

fun <K: Any, V: Any> Map<K, V>.assertValueExists(
    key: K,
    message: String = "Value [$key] not found in map"
): V = get(key) ?: fail(message)

fun BigDecimal?.testRounding(scale: Int): BigDecimal? =
    this?.stripTrailingZeros()?.setScale(scale, RoundingMode.HALF_EVEN)
