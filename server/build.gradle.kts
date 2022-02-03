import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    Plugins.SpringBoot.addTo(this)
    Plugins.SpringDependencyManagement.addTo(this)
    kotlin("plugin.spring") version Versions.Kotlin
}

java.sourceCompatibility = JavaVersion.VERSION_11

configurations.all {
    // CVE-2021-44228 mitigation requires log4j to be greater than 2.15.0 (NOTE gradle 8 will remove VersionNumber but 7.3.x should have a builtin fix for this issue)
    exclude(group = "log4j") // ancient versions of log4j use this group
    val requiredVersion = net.swiftzer.semver.SemVer.parse("2.15.0")
    resolutionStrategy.eachDependency {
        if (requested.group == "org.apache.logging.log4j") {
            requested.version
                ?.let(net.swiftzer.semver.SemVer::parse)
                ?.takeIf { it < requiredVersion }
                ?.also {
                    useVersion("2.15.0")
                    because("CVE-2021-44228")
                }
        }
    }
}

dependencyManagement {
    applyMavenExclusions(false)
}

dependencies {
    // Implementations
    implementation(project(":migrations"))
    implementation(project(":proto"))
    listOf(
        Dependencies.BouncyCastle,
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
        Dependencies.Provenance.ScopeSdk,
        Dependencies.Provenance.ScopeUtil,
        Dependencies.Scarlet.MessageAdapterMoshi,
        Dependencies.Scarlet.Scarlet,
        Dependencies.Scarlet.StreamAdapterRxJava,
        Dependencies.Scarlet.WebSocketOkHttp,
        Dependencies.SpringBoot.AutoConfigure,
        Dependencies.SpringBoot.Starter,
        Dependencies.SpringBoot.StarterActuator,
        Dependencies.SpringBoot.StarterAOP,
        Dependencies.SpringBoot.StarterDataRedis,
        Dependencies.SpringBoot.StarterJetty,
        Dependencies.SpringBoot.StarterValidation,
        Dependencies.SpringBoot.StarterWeb,
        Dependencies.SpringIntegration.Redis,
    ).forEach { it.implementation(this) }

    // Spring Config
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    listOf(
        TestDependencies.Kotlin.CoroutinesTest,
        TestDependencies.Kotlin.KotlinTest,
        TestDependencies.MockK.MockK,
        TestDependencies.MockK.SpringMockK,
        TestDependencies.TestContainers.JUnitJupiter,
        TestDependencies.TestContainers.MockServer,
        TestDependencies.TestContainers.MockServerClient,
        TestDependencies.TestContainers.Postgres,
        TestDependencies.TestContainers.TestContainers,
        TestDependencies.SpringBoot.StarterTest,
    ).forEach { it.testImplementation(this) }
}

// Let gradle know that the integration test directory is for test runnin'
sourceSets {
    create("integrationTest") {
        compileClasspath += main.get().output + test.get().output + configurations.testCompileClasspath + configurations.testCompileOnly
        runtimeClasspath += main.get().output + test.get().output + compileClasspath
        java.srcDir("integrationTest")
    }
}

val testListener = ProjectTestLoggingListener(project)

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("junit-jupiter")
    }
    addTestListener(testListener)
    // Force tests to run every single time even if up to date
    outputs.upToDateWhen { false }
}

tasks.bootRun {
    // Use the provided SPRING_PROFILES_ACTIVE env for the bootRun task, otherwise default to a development environment
    args("--spring.profiles.active=${System.getenv("SPRING_PROFILES_ACTIVE") ?: "development"}")
}

tasks.register<Test>("integrationTest") {
    description = "Run integration tests"
    group = "verification"
    // Points to where the test files live
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    // Tell the integration test suite where it gains files from. Includes test classpath for mock utilities
    classpath = sourceSets["main"].runtimeClasspath + sourceSets["test"].runtimeClasspath + sourceSets["integrationTest"].runtimeClasspath
    testLogging {
        // Shows all application logs as integration tests execute. Uncomment if debugging needed:
        //showStandardStreams = true
        exceptionFormat = TestExceptionFormat.FULL
    }
    useJUnitPlatform {
        excludeTags = setOf("intTest")
        includeEngines = setOf("junit-jupiter", "junit-vintage")
    }
    // Ensure test containers are enabled by default if none is provided
    systemProperty(
        "spring.profiles.active",
        System.getenv("SPRING_PROFILES_ACTIVE") ?: "development,test-containers",
    )
}
