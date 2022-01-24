package tech.figure.invoice.util.extension

import org.junit.jupiter.api.Test
import tech.figure.invoice.UtilProtosBuilders
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.TimeZone
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.junit5.JUnit5Asserter.assertEquals
import kotlin.test.junit5.JUnit5Asserter.assertNull

class ProtoExtensionsTest {
    @BeforeTest
    fun before() {
        // Establish UTC for standard output in timestamps
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @Test
    fun testUuidExtensions() {
        val javaUuid = UUID.randomUUID()
        assertEquals(
            expected = javaUuid.toString(),
            actual = javaUuid.toProtoUuid().value,
            message = "Expected the value of the proto uuid to equal the java uuid in string form",
        )
        assertEquals(
            expected = javaUuid,
            actual = javaUuid.toProtoUuid().toUuid(),
            message = "Expected the java uuid to equal itself when going round trip from java to java",
        )
        assertEquals(
            expected = javaUuid,
            actual = javaUuid.toString().toProtoUuid().toUuid(),
            message = "Expected the java uuid in stringified form to survive a full trip to proto uuid",
        )
        val protoUuid = UtilProtosBuilders.UUID { value = UUID.randomUUID().toString() }
        assertEquals(
            expected = protoUuid.value,
            actual = protoUuid.toUuid().toString(),
            message = "Expected the value of the proto uuid to equate to the stringified version of the java uuid after converting proto -> java",
        )
        assertEquals(
            expected = protoUuid,
            actual = protoUuid.toUuid().toProtoUuid(),
            message = "Expected the proto uuid to equal itself after a round trip from proto to proto"
        )
        val invalidProtoUuid = UtilProtosBuilders.UUID { value = "no u" }
        assertNull(
            actual = invalidProtoUuid.toUuidOrNull(),
            message = "Expected an invalid proto uuid to equate to null after an attempted conversion",
        )
    }

    @Test
    fun testDateExtensions() {
        val javaDate = LocalDate.now()
        assertEquals(
            expected = javaDate.toString(),
            actual = javaDate.toProtoDate().value,
            message = "Expected the value of the proto date to equal the java date in string form",
        )
        assertEquals(
            expected = javaDate,
            actual = javaDate.toProtoDate().toLocalDate(),
            message = "Expected the java date to equal itself when going round trip from java to java",
        )
        val protoDate = UtilProtosBuilders.Date { value = LocalDate.now().toString() }
        assertEquals(
            expected = protoDate.value,
            actual = protoDate.toLocalDate().toString(),
            message = "Expected the value of the proto date to equate to the stringified version of the java date after converting proto -> java",
        )
        assertEquals(
            expected = protoDate,
            actual = protoDate.toLocalDate().toProtoDate(),
            message = "Expected the proto date to equal itself after a round trip from proto to proto"
        )
        val invalidProtoDate = UtilProtosBuilders.Date { value = "2022-01-012" }
        assertNull(
            actual = invalidProtoDate.toLocalDateOrNull(),
            message = "Expected an invalid proto date to equate to null after an attempted conversion",
        )
    }

    @Test
    fun testTimestampExtensions() {
        val javaTimestamp = OffsetDateTime.now()
        assertEquals(
            expected = javaTimestamp,
            actual = javaTimestamp.toProtoTimestamp().toOffsetDateTime(),
            message = "Expected the java timestamp to equal itself when going round trip from java to java",
        )
        val protoTimestamp = OffsetDateTime.now().toProtoTimestamp()
        assertEquals(
            expected = protoTimestamp,
            actual = protoTimestamp.toOffsetDateTime().toProtoTimestamp(),
            message = "Expected the proto timestamp to equal itself after a round trip from proto to proto"
        )
    }

    @Test
    fun testDecimalExtensions() {
        // Test a bunch of different possibilities
        val sourceValues = listOf(
            "1.05",
            "0.00000000000000000001",
            "-0.000000000000000000001",
            "4000",
            "4000000000000000000000000000.00000000000000",
        )
        sourceValues.map { it to BigDecimal(it) }.forEach { (source, javaBigDecimal) ->
            assertEquals(
                expected = source,
                actual = javaBigDecimal.toPlainString(),
                message = "Expected the plain string value to equate to the original input value",
            )
            assertEquals(
                expected = javaBigDecimal.toPlainString(),
                actual = javaBigDecimal.toProtoDecimal().value,
                message = "Expected the decimal amount to equate to the plain string version of the BigDecimal",
            )
            assertEquals(
                expected = javaBigDecimal,
                actual = javaBigDecimal.toProtoDecimal().toBigDecimal(),
                message = "Expected the big decimal to survive a round trip",
            )
        }
        sourceValues.map { UtilProtosBuilders.Decimal { value = it } }.forEach { protoDecimal ->
            assertEquals(
                expected = protoDecimal.value,
                actual = protoDecimal.toBigDecimal().toPlainString(),
                message = "Expected the value of the proto decimal to equate to the stringified version of the BigDecimal after converting proto -> java",
            )
            assertEquals(
                expected = protoDecimal,
                actual = protoDecimal.toBigDecimal().toProtoDecimal(),
                message = "Expected the proto decimal to equal itself after a round trip from proto to proto"
            )
        }
        val invalidProtoDecimal = UtilProtosBuilders.Decimal { value = "This ain't no decimal" }
        assertNull(
            actual = invalidProtoDecimal.toBigDecimalOrNull(),
            message = "Expected an invalid proto decimal to equate to null after an attempted conversion",
        )
    }
}
