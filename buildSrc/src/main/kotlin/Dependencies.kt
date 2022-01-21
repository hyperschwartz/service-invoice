import org.gradle.api.plugins.ObjectConfigurationAction
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.ScriptHandlerScope
import org.gradle.kotlin.dsl.exclude
import org.gradle.plugin.use.PluginDependenciesSpec

object Versions {
    const val Exposed = "0.37.3"
    const val Flyway = "8.0.2"
    const val FrontendHelperPlugin = "6.0.0"
    // Communication with the PbClient requires GRPC to remain versioned in tandem with "ProvenanceClient." Be sure to update this alongside it
    const val Grpc = "1.42.0"
    const val Hikari = "4.0.3"
    const val Jackson = "2.12.5"
    const val JacksonProtobuf = "0.9.12"
    const val Kotlin = "1.5.31"
    const val KotlinCoroutines = "1.5.2"
    const val KotlinLogging = "2.0.11"
    const val Mockk = "1.12.0"
    const val Node = "14.17.3"
    const val Postgres = "42.2.19"
    const val Protobuf = "3.6.1"
    const val ProtobufPlugin = "0.8.16"
    // Both client and proto libraries have coupled versions. Found via inspecting dependencies brought in via client
    const val ProvenanceClient = "1.0.1"
    const val ProvenanceHdWallet = "0.1.9"
    // This version is pinned because ProvenanceClient implementation does not bring it along, and matching it allows access to query protos
    const val ProvenanceProto = "1.7.0-0.0.2"
    const val SpringBoot = "2.5.6"
    const val SpringDependencyManagementPlugin = "1.0.11.RELEASE"
    const val SpringMockk = "3.0.1"
}

object Plugins {
    // Kotlin
    val Kotlin = PluginSpec("kotlin", Versions.Kotlin)

    // 3rd Party
    val Flyway = PluginSpec("org.flywaydb.flyway", Versions.Flyway)
    val FrontendHelper = PluginSpec("org.siouan.frontend-jdk11", Versions.FrontendHelperPlugin)
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
        val KotlinTest = DependencySpec("org.jetbrains.kotlin:kotlin-test", Versions.Kotlin)
    }

    // Spring
    object SpringBoot {
        val Starter = DependencySpec("org.springframework.boot:spring-boot-starter")
        val StarterAOP = DependencySpec("org.springframework.boot:spring-boot-starter-aop")
        val StarterJetty = DependencySpec("org.springframework.boot:spring-boot-starter-jetty")
        val StarterValidation = DependencySpec("org.springframework.boot:spring-boot-starter-validation")
        val StarterWeb = DependencySpec("org.springframework.boot:spring-boot-starter-web")
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
        val ProtobufModule = DependencySpec("com.hubspot.jackson:jackson-datatype-protobuf", Versions.JacksonProtobuf)
    }

    // Database
    object Database {
        val Postgres = DependencySpec("org.postgresql:postgresql", Versions.Postgres)
        val Hikari = DependencySpec("com.zaxxer:HikariCP", Versions.Hikari)
        val Flyway = DependencySpec("org.flywaydb:flyway-core:${Versions.Flyway}")
        val Exposed = DependencySpec("org.jetbrains.exposed:exposed-core", Versions.Exposed)
        val ExposedDao = DependencySpec("org.jetbrains.exposed:exposed-dao", Versions.Exposed)
        val ExposedJdbc = DependencySpec("org.jetbrains.exposed:exposed-jdbc", Versions.Exposed)
    }

    // Logging
    val KotlinLogging = DependencySpec("io.github.microutils:kotlin-logging-jvm", Versions.KotlinLogging)

    // Protobuf
    object Protobuf {
        val Java = DependencySpec("com.google.protobuf:protobuf-java", Versions.Protobuf)
        val JavaUtil = DependencySpec("com.google.protobuf:protobuf-java-util", Versions.Protobuf)
    }

    // Provenance
    object Provenance {
        val ProvenanceGrpcClient = DependencySpec("io.provenance.client:pb-grpc-client-kotlin", Versions.ProvenanceClient)
        val ProvenanceHdWallet = DependencySpec("io.provenance.hdwallet:hdwallet", Versions.ProvenanceHdWallet)
        val ProvenanceProto = DependencySpec("io.provenance.protobuf:pb-proto-java", Versions.ProvenanceProto)
    }

    // GRPC (for Provenance - required dependencies because Prov stuff only brings in GRPC for runtime)
    object Grpc {
        val GrpcProtobuf = DependencySpec("io.grpc:grpc-protobuf", Versions.Grpc)
        val GrpcStub = DependencySpec("io.grpc:grpc-stub", Versions.Grpc)
    }

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
    val isTransitive: Boolean = true,
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
        applyDependency("implementation", handler)
    }

    fun api(handler: DependencyHandlerScope) {
        applyDependency("api", handler)
    }

    fun testImplementation(handler: DependencyHandlerScope) {
        applyDependency("testImplementation", handler)
    }

    fun toDependencyNotation(): String =
        listOfNotNull(
            name,
            version.takeIf { it.isNotEmpty() }
        ).joinToString(":")

    private fun applyDependency(type: String, handler: DependencyHandlerScope) {
        val spec = this
        with(handler) {
            type.invoke(spec.toDependencyNotation()) {
                isChanging = spec.isChanging
                isTransitive = spec.isTransitive
                spec.exclude.forEach { excludeDependencyNotation ->
                    val (group, module) = excludeDependencyNotation.split(":", limit = 2)
                    this.exclude(group = group, module = module)
                }
            }
        }
    }
}
