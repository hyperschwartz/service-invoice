import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.ofSourceSet
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.siouan.frontendgradleplugin.infrastructure.gradle.RunNpm

plugins {
    id("java")
    // Enables NPM installation via gradle
    Plugins.FrontendHelper.addTo(this)
    Plugins.Protobuf.addTo(this)
}

dependencies {
    listOf(
        Dependencies.Protobuf.Java,
        Dependencies.Protobuf.JavaUtil,
    ).forEach { it.implementation(this) }
}

frontend {
    // Ensure the plugin automatically downloads node/npm for us
    nodeDistributionProvided.set(false)
    // Target node version to ensure compatibility
    nodeVersion.set(Versions.Node)
}

// Install all npm deps required for transpiling protobuf to javascript and typescript
tasks.register<RunNpm>("installNpmDependencies") {
    dependsOn(tasks.named("installNode"))
    script.set("install")
}

// Builds the typescript files. See scripts/gen-typescript.sh
// For this to work on a remote build resource, protoc must be on the path for the container to properly execute the
// script
tasks.register<RunNpm>("generateTypescriptProto") {
    dependsOn(tasks.named("installNpmDependencies"))
    script.set("run build-proto")
}

protobuf {
    protoc {
        artifact = "${Plugins.Protobuf.id}:protoc:${Versions.Protobuf}"
    }
    generateProtoTasks {
        all().forEach { task ->
            // Ensure typescript files are generated alongside the java files
            task.dependsOn("generateTypescriptProto")
            task.plugins { ofSourceSet("main") }
        }
    }
}
