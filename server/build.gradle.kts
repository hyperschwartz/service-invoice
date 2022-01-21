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
    // Implementations
    implementation(project(":migrations"))
    implementation(project(":proto"))
    listOf(
        Dependencies.Database.Exposed,
        Dependencies.Database.ExposedDao,
        Dependencies.Database.ExposedJdbc,
        Dependencies.Database.Flyway,
        Dependencies.Database.Hikari,
        Dependencies.Database.Postgres,
        Dependencies.Feign.FeignJackson,
        Dependencies.Grpc.GrpcProtobuf,
        Dependencies.Grpc.GrpcStub,
        Dependencies.Jackson.KotlinModule,
        Dependencies.Jackson.ProtobufModule,
        Dependencies.Kotlin.AllOpen,
        Dependencies.Kotlin.CoroutinesCoreJvm,
        Dependencies.Kotlin.CoroutinesJdk8,
        Dependencies.Kotlin.CoroutinesReactor,
        Dependencies.Kotlin.Reflect,
        Dependencies.Kotlin.StdLibJdk8,
        Dependencies.KotlinLogging,
        Dependencies.Protobuf.Java,
        Dependencies.Protobuf.JavaUtil,
        Dependencies.Provenance.ProvenanceGrpcClient,
        Dependencies.Provenance.ProvenanceHdWallet,
        Dependencies.Provenance.ProvenanceProto,
        Dependencies.SpringBoot.Starter,
        Dependencies.SpringBoot.StarterAOP,
        Dependencies.SpringBoot.StarterJetty,
        Dependencies.SpringBoot.StarterValidation,
        Dependencies.SpringBoot.StarterWeb,
    ).forEach { it.implementation(this) }

    // Spring Config
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    listOf(
        Dependencies.Kotlin.CoroutinesTest,
        Dependencies.Kotlin.KotlinTest,
        Dependencies.Mockk,
        Dependencies.SpringBoot.StarterTest,
        Dependencies.SpringMockk,
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
