import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

// ------------------------------------------------------------------------------------------------------------------ //

plugins {
    java
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.versions)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
}

allprojects {
    group = "critter.plains"
    version = "1.0.0"

    repositories {
        mavenCentral()
        mavenLocal()
    }

    pluginManager.withPlugin("java") {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            compilerOptions {
                languageVersion = KotlinVersion.KOTLIN_2_0
                apiVersion = KotlinVersion.KOTLIN_2_0
                jvmTarget = JvmTarget.JVM_17
            }
        }
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation(project(":critters"))
    implementation(project(":visualization"))
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(libs.junit)
}

application {
    mainClass = "MainKt"
}

// ------------------------------------------------------------------------------------------------------------------ //

tasks {
    named<ShadowJar>("shadowJar") {
        manifest {
            attributes["Main-Class"] = "MainKt"
            attributes["Implementation-Version"] = project.version
        }
        minimize {
            exclude(dependency("org.openrndr:openrndr-gl3:.*"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-reflect:.*"))
            exclude(dependency("org.slf4j:slf4j-simple:.*"))
            exclude(dependency("org.apache.logging.log4j:log4j-slf4j2-impl:.*"))
            exclude(dependency("com.fasterxml.jackson.core:jackson-databind:.*"))
            exclude(dependency("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:.*"))
            exclude(dependency("org.bytedeco:.*"))
        }
    }

    dependencyUpdates {
        gradleReleaseChannel = "current"

        val nonStableKeywords = listOf("alpha", "beta", "rc")

        fun isNonStable(version: String) = nonStableKeywords.any {
            version.lowercase().contains(it)
        }

        rejectVersionIf {
            isNonStable(candidate.version) && !isNonStable(currentVersion)
        }
    }
}
