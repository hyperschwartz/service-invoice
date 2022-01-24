package tech.figure.invoice.config.database

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.Pattern

@ConstructorBinding
@ConfigurationProperties(prefix = "database")
@Validated
data class DatabaseProperties(
    val name: String,
    val username: String,
    val password: String,
    val host: String,
    val port: String,
    val schema: String,
    @Pattern(regexp = "\\d{1,2}") val connectionPoolSize: String,
)
