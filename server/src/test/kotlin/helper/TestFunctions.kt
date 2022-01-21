package helper

import kotlin.test.fail

fun <T> assertSucceeds(message: String = "Block failed to execute without exception", block: () -> T): T = try {
    block()
} catch (e: Exception) {
    fail(message, e)
}

