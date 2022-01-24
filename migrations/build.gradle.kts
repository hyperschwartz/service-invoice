buildscript {
    Dependencies.Database.Postgres.classpath(this)
}

plugins {
    Plugins.Flyway.addTo(this)
}

val postgresHost = System.getenv("DB_HOST") ?: "127.0.0.1"
val postgresPort = System.getenv("DB_PORT") ?: "5432"

flyway {
    url = "jdbc:postgresql://$postgresHost:$postgresPort/invoice-db"
    driver = "org.postgresql.Driver"
    user = "postgres"
    password = "password1"
    schemas = arrayOf("invoice")
    locations = arrayOf("filesystem:${projectDir}/src/main/resources/db/migration", "db/migration")
}
