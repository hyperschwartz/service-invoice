package io.provenance.invoice.util.enums

import com.google.protobuf.Message
import com.google.protobuf.TextFormat
import io.provenance.invoice.util.enums.StringTypeConverterEnum.ConverterDetail
import io.provenance.invoice.util.extension.deriveDefaultInstanceI
import io.provenance.invoice.util.extension.elvisI
import io.provenance.invoice.util.extension.ifNullI
import io.provenance.invoice.util.extension.toBooleanIgnoreCaseI
import io.provenance.invoice.util.extension.toLocalDateOrNullI
import io.provenance.invoice.util.extension.toOffsetDateTimeOrNullI
import io.provenance.invoice.util.extension.toUuidOrNullI
import mu.KLogging
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * A conversion assistant enum for dynamically converting a string to a type.  Allows for portable definitions on how to
 * convert stringified map values into typed values.
 */
enum class StringTypeConverterEnum(val converterDetail: ConverterDetail) {
    STRING(String::class convertWith { it }),
    BOOLEAN(Boolean::class convertWith { it.toBooleanIgnoreCaseI() }),
    UUID(java.util.UUID::class convertWith { it.toUuidOrNullI() }),
    INT(Int::class convertWith { it.toIntOrNull() }),
    LONG(Long::class convertWith { it.toLongOrNull() }),
    DOUBLE(Double::class convertWith { it.toDoubleOrNull() }),
    BIG_DECIMAL(BigDecimal::class convertWith { it.toBigDecimalOrNull() }),
    LOCAL_DATE(LocalDate::class convertWith { it.toLocalDateOrNullI() }),
    OFFSET_DATE_TIME(OffsetDateTime::class convertWith { it.toOffsetDateTimeOrNullI() }),
    ;

    companion object : KLogging() {
        val TYPE_MAP: Map<KClass<*>, StringTypeConverterEnum> by lazy { values().associateBy { it.converterDetail.type } }

        val VALID_KOTLIN_TYPES: Set<KClass<*>> by lazy { TYPE_MAP.keys }

        inline fun <reified T> forTypeOrNull(): StringTypeConverterEnum? = TYPE_MAP.get(T::class)

        inline fun <reified T> forType(): StringTypeConverterEnum = forTypeOrNull<T>()
            ?: throw IllegalArgumentException("No StringTypeConverterEnum mapping exists for type [${T::class.qualifiedName}]")

        /**
         * Attempts to find a type mapping for the given T and convert the input String to a T with it.
         * If the type can't be properly derived via the mapped conversion function, or the type isn't supported, null will be returned.
         * Additionally supports enums and protobuf Messages via sorcery to intercept any value with subclass checks.
         */
        @Suppress("UNCHECKED_CAST")
        inline fun <reified T: Any> convertStringOrNull(value: String, customConverters: Collection<ConverterDetail> = emptyList()): T? = if (T::class.isSubclassOf(Enum::class)) {
            T::class.java.enumConstants.firstOrNull { (it as? Enum<*>)?.name == value }
        } else if (T::class.isSubclassOf(Message::class)) {
            (T::class as? KClass<Message>)?.deriveDefaultInstanceI()?.toBuilder()?.let { messageBuilder ->
                TextFormat.getParser().merge(value, messageBuilder)
                messageBuilder.build() as? T
            }
        } else {
            customConverters
                .singleOrNull { it.type == T::class }
                .elvisI { forTypeOrNull<T>()?.converterDetail }
                .ifNullI { logger.error("Unsupported type [${T::class.qualifiedName}] was provided to ${StringTypeConverterEnum::class.simpleName}.convertStringOrNull. Please add support for this type, if appropriate, or fix the call site to use a valid type") }
                ?.converter
                ?.invoke(value) as? T // Safecast to T? required because Kotlin generics aren't powerful enough!
        }
    }
    data class ConverterDetail(
        val type: KClass<*>,
        val converter: (String) -> Any?
    )
}

private infix fun <T: Any> KClass<T>.convertWith(converter: (String) -> T?): ConverterDetail = ConverterDetail(this, converter)

