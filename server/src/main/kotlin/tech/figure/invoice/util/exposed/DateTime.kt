package tech.figure.invoice.util.exposed

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Function
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.Table
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

fun <T : Table> T.offsetDatetime(name: String): Column<OffsetDateTime> = registerColumn(name, OffsetDateTimeColumnType())

class OffsetDateTimeColumnType : ColumnType() {
    override fun sqlType(): String = "TIMESTAMPTZ"

    override fun valueFromDB(value: Any): Any = when (value) {
        is java.sql.Date -> OffsetDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault())
        is java.sql.Timestamp -> OffsetDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault())
        else -> value
    }
}

class CurrentOffsetDateTime : Function<OffsetDateTime>(OffsetDateTimeColumnType()) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder { append("CURRENT_TIMESTAMP") }
}

fun <T : Table> T.offsetTime(name: String): Column<OffsetTime> = registerColumn(name, OffsetTimeColumnType())

class OffsetTimeColumnType : ColumnType() {
    override fun sqlType() = "TIMETZ"

    override fun readObject(rs: ResultSet, index: Int): Any? {
        return OffsetTime.parse(rs.getString(index), psqlTimeTzFormatter)
    }

    @Suppress("DEPRECATION")
    override fun valueFromDB(value: Any): Any = when (value) {
        is java.sql.Time -> OffsetTime.of(value.toLocalTime(), ZoneOffset.ofHours(-(value.timezoneOffset / 60)))
        else -> value
    }
}

val psqlTimeTzFormatter: DateTimeFormatter = DateTimeFormatterBuilder()
    .append(DateTimeFormatter.ISO_LOCAL_TIME)
    .parseCaseInsensitive()
    .appendOffset("+HH:mm", "Z")
    .toFormatter()
