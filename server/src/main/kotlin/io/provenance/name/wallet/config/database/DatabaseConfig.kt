package io.provenance.name.wallet.config.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

@Configuration
@EnableConfigurationProperties(value = [DatabaseProperties::class])
class DatabaseConfig {
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
}
