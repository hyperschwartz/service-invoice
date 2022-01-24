package tech.figure.invoice.util.exposed

import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.ExpressionWithColumnType
import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.LiteralOp
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.TextColumnType
import org.jetbrains.exposed.sql.append
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import org.postgresql.util.PGobject
import java.lang.reflect.ParameterizedType

class JsonContains(private val expr: Expression<*>, private val attribute: LiteralOp<*>, val equals: LiteralOp<*>) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        return queryBuilder {
            append(expr, " -> ", attribute, " @> ", equals)
        }
    }
}

infix fun <T : Any> Expression<T>.jsonbArray(expression: Expression<T>): Expression<T> =
    JsonbArray(this, expression)

infix fun <T : Any> Expression<T>.jsonbArray(expression: String): Expression<T> =
    JsonbArray(this, JsonbExpression(expression))

infix fun <T : Any> String.jsonbArray(expression: Expression<T>): Expression<T> =
    JsonbArray(JsonbExpression(this), expression)

infix fun <T : Any> String.jsonbArray(expression: String): Expression<T> =
    JsonbArray(JsonbExpression(this), JsonbExpression(expression))

class JsonbArray<T : Any>(private val expr1: Expression<*>, private val expr2: Expression<*>) : Expression<T>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        return queryBuilder {
            append(expr1, " -> ", expr2)
        }
    }
}

infix fun <T : Any, R : Any> Expression<T>.jsonbValue(expression: ExpressionWithColumnType<R>): ExpressionWithColumnType<R> =
    JsonbValue(this, expression)

infix fun <T : Any> Expression<T>.jsonbValue(expression: String): ExpressionWithColumnType<*> =
    JsonbValue(this, JsonbExpression(expression))

infix fun <T : Any> String.jsonbValue(expression: ExpressionWithColumnType<T>): ExpressionWithColumnType<T> =
    JsonbValue(JsonbExpression(this), expression)

infix fun String.jsonbValue(expression: String): ExpressionWithColumnType<*> =
    JsonbValue(JsonbExpression(this), JsonbExpression(expression))

class JsonbValue<T : Any>(private val expr1: Expression<*>, private val expr2: ExpressionWithColumnType<T>) : ExpressionWithColumnType<T>() {
    override val columnType: IColumnType
        get() = expr2.columnType

    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        return queryBuilder {
            append(expr1, " ->> ", expr2)
        }
    }
}

infix fun Expression<*>.jsonbContains(expression: String): Op<Boolean> =
    JsonbContains(this, JsonbExpression(expression))

infix fun String.jsonbContains(expression: String): Op<Boolean> =
    JsonbContains(JsonbExpression(this), JsonbExpression(expression))

class JsonbContains(private val expr1: Expression<*>, val expr2: JsonbExpression) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder {
            append(expr1, " @> ", expr2)
        }
    }
}

class JsonbExpression(private val value: String) : ExpressionWithColumnType<String>() {
    override val columnType: IColumnType
        get() = TextColumnType()

    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder {
            append(value)
        }
    }
}

inline fun <T : Table, reified R : Any> T.jsonb(name: String, objectMapper: ObjectMapper) =
    registerColumn<R>(name, object : JsonBColumnType<R>(objectMapper) {})

abstract class JsonBColumnType<T : Any>(private val objectMapper: ObjectMapper) : ColumnType() {
    @Suppress("UNCHECKED_CAST")
    val clazz: Class<T> = (this::class.java.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<T>

    override fun sqlType() = "JSONB"

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        value?.let { objectMapper.writeValueAsString(it) }
            .let {
                PGobject().apply {
                    type = "jsonb"
                    this.value = it
                }
            }.run {
                stmt[index] = this
            }
    }

    override fun valueFromDB(value: Any): T {
        if (value is PGobject) {
            val json = value.value
            return objectMapper.readValue(json, clazz)
        }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }
}
