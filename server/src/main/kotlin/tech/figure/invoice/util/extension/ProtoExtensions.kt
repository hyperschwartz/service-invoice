package tech.figure.invoice.util.extension

import com.fasterxml.jackson.databind.node.ObjectNode
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
import com.google.protobuf.util.JsonFormat
import com.google.protobuf.util.JsonFormat.TypeRegistry
import com.google.protobuf.util.Timestamps
import tech.figure.invoice.UtilProtos
import tech.figure.invoice.UtilProtos.Date
import tech.figure.invoice.UtilProtos.DateOrBuilder
import tech.figure.invoice.UtilProtos.Decimal
import tech.figure.invoice.UtilProtos.DecimalOrBuilder
import tech.figure.invoice.config.app.ConfigurationUtil.DEFAULT_PROVENANCE_TYPE_REGISTRY
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.functions

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
fun DecimalOrBuilder.toBigDecimal(): BigDecimal = BigDecimal(this.value)
fun DecimalOrBuilder.toBigDecimalOrNull(): BigDecimal? = tryOrNull { toBigDecimal() }
fun DecimalOrBuilder.toBigDecimalOrZero(): BigDecimal = toBigDecimalOrNull() ?: BigDecimal.ZERO

//
// Any
//
fun <T: MessageOrBuilder> T.toProtoAny(): com.google.protobuf.Any = com.google.protobuf.Any.pack(this.buildDynamic(), "")
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
inline fun <reified T: Message> com.google.protobuf.Any.typedUnpack(): T = this.unpack(T::class.java)

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
    throw IllegalArgumentException("Failed to construct builder for type [${this::class.qualifiedName}]", e)
}

/**
 * Reflection "hack" to get the default instance of a Message from the KClass instance.
 * Note: This is practically guaranteed to be a slow invocation, so using this is a guaranteed performance hit.
 */
@Suppress("UNCHECKED_CAST")
fun <T: Message> KClass<T>.deriveDefaultInstance(): T = this
    .functions
    .singleOrNull { it.name == "getDefaultInstance" }
    .checkNotNull { "Could not resolve static default getDefaultInstance function for protobuf Message class ${this.qualifiedName}" }
    .call()
    .let { it as? T }
    .checkNotNull { "Unable to cast default instance of class [${this.qualifiedName}] to a usable format" }

inline fun <reified T: Message.Builder> T.mergeFromJson(json: String, registry: TypeRegistry? = null): T = this.also { messageBuilder ->
    try {
        JsonFormat.parser()
            .let { parser -> registry?.let(parser::usingTypeRegistry) ?: parser }
            .merge(json, messageBuilder)
    } catch (e: Exception) {
        throw IllegalArgumentException("Failed to merge json value to source type [${T::class.java}]")
    }
}

inline fun <reified T: Message.Builder> T.mergeFromJson(json: ObjectNode, registry: TypeRegistry? = null): T =
    mergeFromJson(json.toString(), registry)

inline fun <reified T: Message.Builder> T.mergeFromJsonProvenance(json: String): T =
    mergeFromJson(json, DEFAULT_PROVENANCE_TYPE_REGISTRY)

inline fun <reified T: Message.Builder> T.mergeFromJsonProvenance(json: ObjectNode): T =
    mergeFromJsonProvenance(json.toString())

inline fun <reified T: Message> T.toJsonProvenance(): String = JsonFormat
    .printer()
    .usingTypeRegistry(DEFAULT_PROVENANCE_TYPE_REGISTRY)
    .print(this)
