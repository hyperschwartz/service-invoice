package io.provenance.invoice.util.extension

import com.google.protobuf.BoolValue
import com.google.protobuf.ByteString
import com.google.protobuf.BytesValue
import com.google.protobuf.Int32Value
import com.google.protobuf.Int64Value
import com.google.protobuf.Message
import com.google.protobuf.MessageOrBuilder
import com.google.protobuf.StringValue
import com.google.protobuf.Timestamp
import com.google.protobuf.TimestampOrBuilder
import com.google.protobuf.util.Timestamps
import io.provenance.invoice.UtilProtos
import io.provenance.invoice.UtilProtos.Date
import io.provenance.invoice.UtilProtos.DateOrBuilder
import io.provenance.invoice.UtilProtos.Decimal
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

//
// UUID
//
fun UUID.toProtoUuid(): UtilProtos.UUID = UtilProtos.UUID.newBuilder().setValue(this.toString()).build()
fun String.toProtoUuid(): UtilProtos.UUID = UUID.fromString(this).toProtoUuid()
fun UtilProtos.UUIDOrBuilder.toUuid(): UUID = UUID.fromString(this.value)
fun UtilProtos.UUIDOrBuilder.toUuidOrNull(): UUID? = tryOrNull { toUuid() }

//
// Date
//
fun LocalDate.toProtoDate(): Date = Date.newBuilder().setValue(this.toString()).build()
fun DateOrBuilder.toLocalDate(): LocalDate = LocalDate.parse(this.value)
fun DateOrBuilder.toLocalDateOrNull(): LocalDate? = tryOrNull { toLocalDate() }

//
// Timestamp
//
fun OffsetDateTime.toProtoTimestamp(): Timestamp = this.toInstant().flowTo(Timestamp.newBuilder()) { instant, builder ->
    builder.seconds = instant.epochSecond.coerceAtLeast(Timestamps.MIN_VALUE.seconds).coerceAtMost(Timestamps.MAX_VALUE.seconds)
    builder.nanos = instant.nano.coerceAtLeast(Timestamps.MIN_VALUE.nanos).coerceAtMost(Timestamps.MAX_VALUE.nanos)
    builder.build()
}
fun TimestampOrBuilder.toOffsetDateTime(): OffsetDateTime = Instant.ofEpochSecond(seconds, nanos.toLong())
    .let { instant -> OffsetDateTime.ofInstant(instant, ZoneId.systemDefault()) }
fun TimestampOrBuilder.toOffsetDateTimeOrNull(): OffsetDateTime? = tryOrNull { toOffsetDateTime() }

//
// Decimal
//
fun BigDecimal.toProtoDecimal(): Decimal = Decimal.newBuilder().setValue(this.toPlainString()).build()
fun String.toProtoDecimal(): Decimal = this.toBigDecimal().toProtoDecimal()
fun String.toProtoDecimalOrNull(): Decimal? = this.toBigDecimalOrNull()?.toProtoDecimal()
fun UtilProtos.DecimalOrBuilder.toBigDecimal(): BigDecimal = BigDecimal(this.value)
fun UtilProtos.DecimalOrBuilder.toBigDecimalOrNull(): BigDecimal? = tryOrNull { toBigDecimal() }

//
// Any
//
fun <T: MessageOrBuilder> T.toProtoAny(): com.google.protobuf.Any = com.google.protobuf.Any.pack(this.buildDynamic())
fun Boolean.toProtoAny(): com.google.protobuf.Any = BoolValue.newBuilder().setValue(this).build().toProtoAny()
fun ByteArray.toProtoAny(): com.google.protobuf.Any = BytesValue.newBuilder().setValue(ByteString.copyFrom(this)).build().toProtoAny()
fun String.toProtoAny(): com.google.protobuf.Any = StringValue.newBuilder().setValue(this).build().toProtoAny()
fun Long.toProtoAny(): com.google.protobuf.Any = Int64Value.newBuilder().setValue(this).build().toProtoAny()
fun Int.toProtoAny(): com.google.protobuf.Any = Int32Value.newBuilder().setValue(this).build().toProtoAny()
fun LocalDate.toProtoAny(): com.google.protobuf.Any = this.toProtoDate().toProtoAny()
fun BigDecimal.toProtoAny(): com.google.protobuf.Any = this.toProtoDecimal().toProtoAny()
fun OffsetDateTime.toProtoAny(): com.google.protobuf.Any = this.toProtoTimestamp().toProtoAny()
fun <T: Any> T.genericToProtoAny(): com.google.protobuf.Any = when (this) {
    is Message -> toProtoAny()
    is Boolean -> toProtoAny()
    is ByteArray -> toProtoAny()
    is String -> toProtoAny()
    is Long -> toProtoAny()
    is Int -> toProtoAny()
    is LocalDate -> toProtoAny()
    is BigDecimal -> toProtoAny()
    is OffsetDateTime -> toProtoAny()
    else -> throw IllegalArgumentException("Unable to convert value of type [${this::class.qualifiedName}] to proto Any. Specified type is not supported")
}

//
// Generic
//
fun <T: MessageOrBuilder> T.isSet(): Boolean = this != this.defaultInstanceForType

inline fun <reified T: Message> MessageOrBuilder.buildDynamic(): T = try {
    if (this is Message.Builder) {
        this.build() as T
    } else {
        this as T
    }
} catch (e: Exception) {
    throw IllegalArgumentException("Failed to build Message type [${this::class.qualifiedName}]", e)
}

inline fun <reified T: Message.Builder> MessageOrBuilder.toBuilderDynamic(): T = try {
    if (this is Message) {
        this.toBuilder() as T
    } else {
        this as T
    }
} catch (e: Exception) {
    throw IllegalArgumentException("Faile to construct builder for type [${this::class.qualifiedName}]", e)
}
