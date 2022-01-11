import org.gradle.api.plugins.ObjectConfigurationAction
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.ScriptHandlerScope
import org.gradle.kotlin.dsl.exclude
import org.gradle.plugin.use.PluginDependenciesSpec

object Versions {
    const val Kotlin = "1.5.31"
    const val KotlinCoroutines = "1.5.2"
    const val ProtobufPlugin = "0.8.16"
    const val SpringBoot = "2.5.6"
    const val SpringDependencyManagementPlugin = "1.0.11.RELEASE"
    const val KotlinLogging = "2.0.11"
    const val Jackson = "2.12.5"
    const val Postgres = "42.2.19"
    const val Flyway = "8.0.2"
    const val Mockk = "1.12.0"
    const val SpringMockk = "3.0.1"
    const val Hikari = "4.0.3"
}

object Plugins {
    // Kotlin
    val Kotlin = PluginSpec("kotlin", Versions.Kotlin)

    // 3rd Party
    val Flyway = PluginSpec("org.flywaydb.flyway", Versions.Flyway)
    val Idea = PluginSpec("idea")
    val Protobuf = PluginSpec("com.google.protobuf", Versions.ProtobufPlugin)
    val SpringBoot = PluginSpec("org.springframework.boot", Versions.SpringBoot)
    val SpringDependencyManagement = PluginSpec("io.spring.dependency-management", Versions.SpringDependencyManagementPlugin)
}

object Dependencies {
    // Kotlin
    object Kotlin {
        val AllOpen = DependencySpec("org.jetbrains.kotlin:kotlin-allopen", Versions.Kotlin)
        val Reflect = DependencySpec("org.jetbrains.kotlin:kotlin-reflect", Versions.Kotlin)
        val StdLibJdk8 = DependencySpec("org.jetbrains.kotlin:kotlin-stdlib-jdk8", Versions.Kotlin)
        val CoroutinesCoreJvm = DependencySpec("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm", Versions.KotlinCoroutines)
        val CoroutinesReactor = DependencySpec("org.jetbrains.kotlinx:kotlinx-coroutines-reactor", Versions.KotlinCoroutines)
        val CoroutinesJdk8 = DependencySpec("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8", Versions.KotlinCoroutines)
        val CoroutinesTest = DependencySpec("org.jetbrains.kotlinx:kotlinx-coroutines-test", Versions.KotlinCoroutines)
        val KotlinTest = DependencySpec("org.jetbrains.kotlin:kotlin-test:${Versions.Kotlin}")
    }

    // Spring
    object SpringBoot {
        val Starter = DependencySpec("org.springframework.boot:spring-boot-starter")
        val StarterWebFlux = DependencySpec(
            name = "org.springframework.boot:spring-boot-starter-webflux",
            exclude = listOf("org.springframework.boot:spring-boot-starter-tomcat")
        )
        val StarterJetty = DependencySpec("org.springframework.boot:spring-boot-starter-jetty")
        val StarterActuator = DependencySpec("org.springframework.boot:spring-boot-starter-actuator")
        val StarterDevTools = DependencySpec("org.springframework.boot:spring-boot-devtools")
        val StarterSecurity = DependencySpec("org.springframework.boot:spring-boot-starter-security")
        val StarterValidation = DependencySpec("org.springframework.boot:spring-boot-starter-validation")

        val StarterTest =
            DependencySpec(
                name = "org.springframework.boot:spring-boot-starter-test",
                exclude = listOf(
                    "org.junit.vintage:junit-vintage-engine",
                    "org.mockito:mockito-core"
                )
            )
    }

    // Jackson
    object Jackson {
        val KotlinModule = DependencySpec("com.fasterxml.jackson.module:jackson-module-kotlin", Versions.Jackson)
    }

    // Database
    object Database {
        val Postgres = DependencySpec("org.postgresql:postgresql", Versions.Postgres)
        val Hikari = DependencySpec("com.zaxxer:HikariCP", Versions.Hikari)
    }

    // Logging
    val KotlinLogging = DependencySpec("io.github.microutils:kotlin-logging-jvm", Versions.KotlinLogging)

    // Testing
    val Mockk = DependencySpec("io.mockk:mockk", Versions.Mockk)
    val SpringMockk = DependencySpec("com.ninja-squad:springmockk", Versions.SpringMockk)
}

data class PluginSpec(
    val id: String,
    val version: String = ""
) {
    fun addTo(scope: PluginDependenciesSpec) {
        scope.also {
            it.id(id).version(version.takeIf { v -> v.isNotEmpty() })
        }
    }

    fun addTo(action: ObjectConfigurationAction) {
        action.plugin(this.id)
    }
}

data class DependencySpec(
    val name: String,
    val version: String = "",
    val isChanging: Boolean = false,
    val exclude: List<String> = emptyList()
) {
    fun plugin(scope: PluginDependenciesSpec) {
        scope.apply {
            id(name).version(version.takeIf { it.isNotEmpty() })
        }
    }

    fun classpath(scope: ScriptHandlerScope) {
        val spec = this
        with(scope) {
            dependencies {
                classpath(spec.toDependencyNotation())
            }
        }
    }

    fun implementation(handler: DependencyHandlerScope) {
        val spec = this
        with(handler) {
            "implementation".invoke(spec.toDependencyNotation()) {
                isChanging = spec.isChanging
                spec.exclude.forEach { excludeDependencyNotation ->
                    val (group, module) = excludeDependencyNotation.split(":", limit = 2)
                    this.exclude(group = group, module = module)
                }
            }
        }
    }

    fun testImplementation(handler: DependencyHandlerScope) {
        val spec = this
        with(handler) {
            "testImplementation".invoke(spec.toDependencyNotation()) {
                isChanging = spec.isChanging
                spec.exclude.forEach { excludeDependencyNotation ->
                    val (group, module) = excludeDependencyNotation.split(":", limit = 2)
                    this.exclude(group = group, module = module)
                }
            }
        }
    }

    fun toDependencyNotation(): String =
        listOfNotNull(
            name,
            version.takeIf { it.isNotEmpty() }
        ).joinToString(":")
}
