plugins {
    kotlin("jvm") version Versions.Kotlin apply false
    id("java")
    id("maven-publish")
}

allprojects {
    group = "io.provenance.tech.wallet.name"
    version = artifactVersion()

    repositories {
        mavenCentral()
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    sourceCompatibility = "11"
    targetCompatibility = "11"

    kotlinOptions {
        freeCompilerArgs = listOf(
            "-Xjsr305=strict",
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
        jvmTarget = "11"
        allWarningsAsErrors = true
    }
}

subprojects {
    apply {
        Plugins.Kotlin.addTo(this)
        Plugins.Idea.addTo(this)
        plugin("java")
    }

    repositories {
        mavenCentral()
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"

        kotlinOptions {
            freeCompilerArgs = listOf(
                "-Xjsr305=strict",
                "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
            )
            jvmTarget = "11"
            allWarningsAsErrors = true
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
