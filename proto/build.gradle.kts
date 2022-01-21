import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.ofSourceSet
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    id("java")
    Plugins.Protobuf.addTo(this)
}

dependencies {
    listOf(
        Dependencies.Protobuf.Java,
        Dependencies.Protobuf.JavaUtil,
    ).forEach { it.implementation(this) }
}

protobuf {
    protoc {
        artifact = "${Plugins.Protobuf.id}:protoc:${Versions.Protobuf}"
    }
    generateProtoTasks {
        all().forEach {
            it.plugins { ofSourceSet("main") }
        }
    }
}
