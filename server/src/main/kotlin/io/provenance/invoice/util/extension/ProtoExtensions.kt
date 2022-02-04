package io.provenance.invoice.util.extension

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
import io.provenance.invoice.config.app.ConfigurationUtil.DEFAULT_PROVENANCE_TYPE_REGISTRY
import io.provenance.invoice.UtilProtos
import io.provenance.invoice.UtilProtos.Date
import io.provenance.invoice.UtilProtos.DateOrBuilder
import io.provenance.invoice.UtilProtos.Decimal
import io.provenance.invoice.UtilProtos.DecimalOrBuilder
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
fun UUID.toProtoUuidI(): UtilProtos.UUID = UtilProtos.UUID.newBuilder().setValue(this.toString()).build()
fun String.toProtoUuidI(): UtilProtos.UUID = UUID.fromString(this).toProtoUuidI()
fun UtilProtos.UUIDOrBuilder.toUuidI(): UUID = UUID.fromString(this.value)
fun UtilProtos.UUIDOrBuilder.toUuidOrNullI(): UUID? = tryOrNullI { toUuidI() }

//
// Date
//
fun LocalDate.toProtoDateI(): Date = Date.newBuilder().setValue(this.toString()).build()
fun DateOrBuilder.toLocalDateI(): LocalDate = LocalDate.parse(this.value)
fun DateOrBuilder.toLocalDateOrNullI(): LocalDate? = tryOrNullI { toLocalDateI() }
fun DateOrBuilder.toOffsetDateTimeI(): OffsetDateTime = toLocalDateI().toOffsetDateTimeI()
fun DateOrBuilder.toOffsetDateTimeOrNullI(): OffsetDateTime? = tryOrNullI { toOffsetDateTimeI() }

//
// Timestamp
//
fun OffsetDateTime.toProtoTimestampI(): Timestamp = this.toInstant().flowToI(Timestamp.newBuilder()) { instant, builder ->
    builder.seconds = instant.epochSecond.coerceAtLeast(Timestamps.MIN_VALUE.seconds).coerceAtMost(Timestamps.MAX_VALUE.seconds)
    builder.nanos = instant.nano.coerceAtLeast(Timestamps.MIN_VALUE.nanos).coerceAtMost(Timestamps.MAX_VALUE.nanos)
    builder.build()
}
fun TimestampOrBuilder.toOffsetDateTimeI(): OffsetDateTime = Instant.ofEpochSecond(seconds, nanos.toLong())
    .let { instant -> OffsetDateTime.ofInstant(instant, ZoneId.systemDefault()) }
fun TimestampOrBuilder.toOffsetDateTimeOrNullI(): OffsetDateTime? = tryOrNullI { toOffsetDateTimeI() }

//
// Decimal
//
fun BigDecimal.toProtoDecimalI(): Decimal = Decimal.newBuilder().setValue(this.toPlainString()).build()
fun String.toProtoDecimalI(): Decimal = this.toBigDecimal().toProtoDecimalI()
fun String.toProtoDecimalOrNullI(): Decimal? = this.toBigDecimalOrNull()?.toProtoDecimalI()
fun DecimalOrBuilder.toBigDecimalI(): BigDecimal = BigDecimal(this.value)
fun DecimalOrBuilder.toBigDecimalOrNullI(): BigDecimal? = tryOrNullI { toBigDecimalI() }
fun DecimalOrBuilder.toBigDecimalOrZeroI(): BigDecimal = toBigDecimalOrNullI() ?: BigDecimal.ZERO

//
// Any
//
fun <T: MessageOrBuilder> T.toProtoAnyI(): com.google.protobuf.Any = com.google.protobuf.Any.pack(this.buildDynamicI(), "")
fun Boolean.toProtoAnyI(): com.google.protobuf.Any = BoolValue.newBuilder().setValue(this).build().toProtoAnyI()
fun ByteArray.toProtoAnyI(): com.google.protobuf.Any = BytesValue.newBuilder().setValue(ByteString.copyFrom(this)).build().toProtoAnyI()
fun String.toProtoAnyI(): com.google.protobuf.Any = StringValue.newBuilder().setValue(this).build().toProtoAnyI()
fun Long.toProtoAnyI(): com.google.protobuf.Any = Int64Value.newBuilder().setValue(this).build().toProtoAnyI()
fun Int.toProtoAnyI(): com.google.protobuf.Any = Int32Value.newBuilder().setValue(this).build().toProtoAnyI()
fun LocalDate.toProtoAnyI(): com.google.protobuf.Any = this.toProtoDateI().toProtoAnyI()
fun BigDecimal.toProtoAnyI(): com.google.protobuf.Any = this.toProtoDecimalI().toProtoAnyI()
fun OffsetDateTime.toProtoAnyI(): com.google.protobuf.Any = this.toProtoTimestampI().toProtoAnyI()
fun <T: Any> T.genericToProtoAnyI(): com.google.protobuf.Any = when (this) {
    is Message -> toProtoAnyI()
    is Boolean -> toProtoAnyI()
    is ByteArray -> toProtoAnyI()
    is String -> toProtoAnyI()
    is Long -> toProtoAnyI()
    is Int -> toProtoAnyI()
    is LocalDate -> toProtoAnyI()
    is BigDecimal -> toProtoAnyI()
    is OffsetDateTime -> toProtoAnyI()
    else -> throw IllegalArgumentException("Unable to convert value of type [${this::class.qualifiedName}] to proto Any. Specified type is not supported")
}
inline fun <reified T: Message> com.google.protobuf.Any.typedUnpackI(): T = this.unpack(T::class.java)

//
// Generic
//
fun <T: MessageOrBuilder> T.isSet(): Boolean = this != this.defaultInstanceForType

inline fun <reified T: Message> MessageOrBuilder.buildDynamicI(): T = try {
    if (this is Message.Builder) {
        this.build() as T
    } else {
        this as T
    }
} catch (e: Exception) {
    throw IllegalArgumentException("Failed to build Message type [${this::class.qualifiedName}]", e)
}

inline fun <reified T: Message.Builder> MessageOrBuilder.toBuilderDynamicI(): T = try {
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
fun <T: Message> KClass<T>.deriveDefaultInstanceI(): T = this
    .functions
    .singleOrNull { it.name == "getDefaultInstance" }
    .checkNotNullI { "Could not resolve static default getDefaultInstance function for protobuf Message class ${this.qualifiedName}" }
    .call()
    .let { it as? T }
    .checkNotNullI { "Unable to cast default instance of class [${this.qualifiedName}] to a usable format" }

inline fun <reified T: Message.Builder> T.mergeFromJsonI(json: String, registry: TypeRegistry? = null): T = this.also { messageBuilder ->
    try {
        JsonFormat.parser()
            .let { parser -> registry?.let(parser::usingTypeRegistry) ?: parser }
            .merge(json, messageBuilder)
    } catch (e: Exception) {
        throw IllegalArgumentException("Failed to merge json value to source type [${T::class.java}]")
    }
}

inline fun <reified T: Message.Builder> T.mergeFromJsonI(json: ObjectNode, registry: TypeRegistry? = null): T =
    mergeFromJsonI(json.toString(), registry)

inline fun <reified T: Message.Builder> T.mergeFromJsonProvenanceI(json: String): T =
    mergeFromJsonI(json, DEFAULT_PROVENANCE_TYPE_REGISTRY)

inline fun <reified T: Message.Builder> T.mergeFromJsonProvenanceI(json: ObjectNode): T =
    mergeFromJsonProvenanceI(json.toString())

inline fun <reified T: Message> T.toJsonProvenanceI(): String = JsonFormat
    .printer()
    .usingTypeRegistry(DEFAULT_PROVENANCE_TYPE_REGISTRY)
    .print(this)
