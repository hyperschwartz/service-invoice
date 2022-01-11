plugins {
    Plugins.SpringBoot.addTo(this)
    Plugins.SpringDependencyManagement.addTo(this)
    kotlin("plugin.spring") version Versions.Kotlin
}

java.sourceCompatibility = JavaVersion.VERSION_11

dependencyManagement {
    applyMavenExclusions(false)
}

dependencies {
    implementation(project(":migrations"))
    listOf(
        Dependencies.Kotlin.AllOpen,
        Dependencies.Kotlin.Reflect,
        Dependencies.Kotlin.StdLibJdk8,
        Dependencies.Kotlin.CoroutinesCoreJvm,
        Dependencies.Kotlin.CoroutinesReactor,
        Dependencies.Kotlin.CoroutinesJdk8,
        Dependencies.SpringBoot.Starter,
        Dependencies.SpringBoot.StarterActuator,
        Dependencies.SpringBoot.StarterJetty,
        Dependencies.SpringBoot.StarterDevTools,
        Dependencies.SpringBoot.StarterValidation,
        Dependencies.SpringBoot.StarterWeb,
        Dependencies.Database.Postgres,
        Dependencies.Database.Hikari,
        Dependencies.Database.Flyway,
        Dependencies.Database.Exposed,
        Dependencies.Database.ExposedDao,
        Dependencies.Database.ExposedJdbc,
        Dependencies.KotlinLogging,
        Dependencies.Jackson.KotlinModule,
    ).forEach { it.implementation(this) }

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    listOf(
        Dependencies.Mockk,
        Dependencies.SpringMockk,
        Dependencies.SpringBoot.StarterTest,
        Dependencies.Kotlin.CoroutinesTest,
        Dependencies.Kotlin.KotlinTest,
    ).forEach { it.testImplementation(this) }
}

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("junit-jupiter")
    }
    testLogging {
        events("passed", "skipped", "failed")
    }
}
