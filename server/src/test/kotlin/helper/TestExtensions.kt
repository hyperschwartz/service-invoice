package helper

import kotlin.test.fail

fun <K: Any, V: Any> Map<K, V>.assertValueExists(
    key: K,
    message: String = "Value [$key] not found in map"
): V = get(key) ?: fail(message)

