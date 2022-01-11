import org.gradle.api.Project

fun Project.artifactVersion(): String = this.findProperty("artifactVersion")?.toString()
    ?: "1.0-snapshot"
