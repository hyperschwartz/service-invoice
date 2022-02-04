package helper

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.assertNotNull
import kotlin.test.fail

fun <K: Any, V: Any> Map<K, V>.assertValueExistsI(
    key: K,
    message: String = "Value [$key] not found in map"
): V = get(key) ?: fail(message)

fun <T: Any> T?.assertNotNullI(message: String? = null): T = assertNotNull(this, message)

fun <T: Any> Collection<T>.assertSingleI(
    message: String = "Collection did not contain a single element",
    predicate: (T) -> Boolean,
): T = this.singleOrNull(predicate).assertNotNullI("$message. Collection size: ${this.size}")

fun <T: Any> Collection<T>.assertSingleI(message: String = "Collection did not contain a single element"): T =
    assertSingleI(message) { true }

fun BigDecimal?.testRoundingI(scale: Int): BigDecimal? =
    this?.stripTrailingZeros()?.setScale(scale, RoundingMode.HALF_EVEN)
