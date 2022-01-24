package tech.figure.invoice.config.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import mu.KLogging
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.sql.Connection
import javax.sql.DataSource

@Configuration
@EnableConfigurationProperties(value = [DatabaseProperties::class])
class DatabaseConfig {
    private companion object : KLogging()

    @Primary
    @Bean
    fun dataSource(dbProperties: DatabaseProperties): DataSource = HikariConfig().let { hikariConfig ->
        hikariConfig.jdbcUrl = "jdbc:postgresql://${dbProperties.host}:${dbProperties.port}/${dbProperties.name}?prepareThreshold=0"
        hikariConfig.username = dbProperties.username
        hikariConfig.password = dbProperties.password
        hikariConfig.schema = dbProperties.schema
        dbProperties.connectionPoolSize.toInt().also { poolSize ->
            hikariConfig.minimumIdle = poolSize / 2
            hikariConfig.maximumPoolSize = poolSize
        }
        HikariDataSource(hikariConfig)
    }

    @Bean("databaseConnect")
    fun databaseConnect(dataSource: DataSource): Database = Database.connect(dataSource)
        .also { TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_READ_COMMITTED }

    @Bean
    fun flyway(dataSource: DataSource): Flyway = Flyway(FluentConfiguration().dataSource(dataSource))

    @Bean
    fun flywayInitializer(flyway: Flyway): FlywayMigrationInitializer = FlywayMigrationInitializer(flyway)

    @Bean("migrationsExecuted")
    fun flywayMigration(dataSource: DataSource, flyway: Flyway): Int {
        flyway.info().all().forEach { logger.info("Flyway migration: ${it.script}") }
        return flyway.migrate().migrationsExecuted
    }
}
