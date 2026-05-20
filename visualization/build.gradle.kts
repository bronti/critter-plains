import org.gradle.internal.os.OperatingSystem
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

/**  ## additional ORX features to be added to this project */
val orxFeatures = setOf<String>(
//  "orx-axidraw",
//  "orx-boofcv",
    "orx-camera",
//  "orx-chataigne",
    "orx-color",
    "orx-composition",
    "orx-compositor",
//  "orx-compute-graph",
//  "orx-compute-graph-nodes",
    "orx-delegate-magic",
//  "orx-dnk3",
//  "orx-easing",
    "orx-envelopes",
//  "orx-expression-evaluator",
//  "orx-fcurve",
//  "orx-fft",
//  "orx-file-watcher",
    "orx-fx",
//  "orx-git-archiver",
//  "orx-gradient-descent",
    "orx-gui",
//  "orx-hash-grid",
    "orx-image-fit",
//  "orx-integral-image",
//  "orx-interval-tree",
//  "orx-jumpflood",
//  "orx-kdtree",
//  "orx-keyframer",
//  "orx-kinect-v1",
//  "orx-kotlin-parser",
//  "orx-marching-squares",
//  "orx-math",
//  "orx-mesh-generators",
//  "orx-midi",
//  "orx-minim",
    "orx-no-clear",
    "orx-noise",
//  "orx-obj-loader",
    "orx-olive",
//  "orx-osc",
//  "orx-palette",
    "orx-panel",
//  "orx-parameters",
//  "orx-poisson-fill",
//  "orx-property-watchers",
//  "orx-quadtree",
//  "orx-rabbit-control",
//  "orx-realsense2",
//  "orx-runway",
    "orx-shade-styles",
//  "orx-shader-phrases",
    "orx-shapes",
    "orx-svg",
//  "orx-syphon",
//  "orx-temporal-blur",
//  "orx-tensorflow",
    "orx-text-writer",
//  "orx-time-operators",
//  "orx-timer",
//  "orx-triangulation",
//  "orx-turtle",
    "orx-video-profiles",
    "orx-view-box",
)

/** ## additional ORML features to be added to this project */
val ormlFeatures = setOf<String>(
//    "orml-blazepose",
//    "orml-dbface",
//    "orml-facemesh",
//    "orml-image-classifier",
//    "orml-psenet",
//    "orml-ssd",
//    "orml-style-transfer",
//    "orml-super-resolution",
//    "orml-u2net",
)

/** ## additional OPENRNDR features to be added to this project */
val openrndrFeatures = setOfNotNull(
    if (DefaultNativePlatform("current").architecture.name != "arm-v8") "video" else null
)

/** ## configure the type of logging this project uses */
enum class Logging { NONE, SIMPLE, FULL }

val applicationLogging = Logging.FULL

// ------------------------------------------------------------------------------------------------------------------ //

plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {

//    implementation(libs.jsoup)
//    implementation(libs.csv)

    /* ORSL dependencies */

//    implementation(libs.orsl.shader.generator)
//    implementation(libs.orsl.extension.color)
//    implementation(libs.orsl.extension.easing)
//    implementation(libs.orsl.extension.gradient)
//    implementation(libs.orsl.extension.noise)
//    implementation(libs.orsl.extension.pbr)
//    implementation(libs.orsl.extension.raymarching)
//    implementation(libs.orsl.extension.sdf)

    implementation(project(":critters"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.slf4j.api)
    implementation(libs.kotlin.logging)

    when (applicationLogging) {
        Logging.NONE -> runtimeOnly(libs.slf4j.nop)
        Logging.SIMPLE -> runtimeOnly(libs.slf4j.simple)
        Logging.FULL -> {
            runtimeOnly(libs.log4j.slf4j2)
            runtimeOnly(libs.log4j.core)
            runtimeOnly(libs.jackson.databind)
            runtimeOnly(libs.jackson.json)
        }
    }
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(libs.junit)
}

// ------------------------------------------------------------------------------------------------------------------ //

class Openrndr {
    val openrndrVersion = libs.versions.openrndr.get()
    val orxVersion = libs.versions.orx.get()
    val ormlVersion = libs.versions.orml.get()

    // choices are "orx-tensorflow-gpu", "orx-tensorflow"
    val orxTensorflowBackend = "orx-tensorflow"

    val currArch = DefaultNativePlatform("current").architecture.name
    val currOs = OperatingSystem.current()
    val os = if (project.hasProperty("targetPlatform")) {
        val supportedPlatforms = setOf("windows", "macos", "linux-x64", "linux-arm64")
        val platform: String = project.property("targetPlatform") as String
        if (platform !in supportedPlatforms) {
            throw IllegalArgumentException("target platform not supported: $platform")
        } else {
            platform
        }
    } else when {
        currOs.isWindows -> "windows"
        currOs.isMacOsX -> when (currArch) {
            "aarch64", "arm-v8" -> "macos-arm64"
            else -> "macos"
        }

        currOs.isLinux -> when (currArch) {
            "x86-64" -> "linux-x64"
            "aarch64" -> "linux-arm64"
            else -> throw IllegalArgumentException("architecture not supported: $currArch")
        }

        else -> throw IllegalArgumentException("os not supported: ${currOs.name}")
    }

    fun orx(module: String) = "org.openrndr.extra:$module:$orxVersion"
    fun orml(module: String) = "org.openrndr.orml:$module:$ormlVersion"
    fun openrndr(module: String) = "org.openrndr:openrndr-$module:$openrndrVersion"
    fun openrndrNatives(module: String) = "org.openrndr:openrndr-$module-natives-$os:$openrndrVersion"
    fun orxNatives(module: String) = "org.openrndr.extra:$module-natives-$os:$orxVersion"

    init {
        dependencies {
            runtimeOnly(openrndr("gl3"))
            runtimeOnly(openrndrNatives("gl3"))
            implementation(openrndr("openal"))
            runtimeOnly(openrndrNatives("openal"))
            implementation(openrndr("application"))
            implementation(openrndr("animatable"))
            implementation(openrndr("extensions"))
            implementation(openrndr("filter"))
            implementation(openrndr("dialogs"))
            if ("video" in openrndrFeatures) {
                implementation(openrndr("ffmpeg"))
                runtimeOnly(openrndrNatives("ffmpeg"))
            }
            for (feature in orxFeatures) {
                implementation(orx(feature))
            }
            for (feature in ormlFeatures) {
                implementation(orml(feature))
            }
            if ("orx-tensorflow" in orxFeatures) runtimeOnly("org.openrndr.extra:$orxTensorflowBackend-natives-$os:$orxVersion")
            if ("orx-kinect-v1" in orxFeatures) runtimeOnly(orxNatives("orx-kinect-v1"))
            if ("orx-olive" in orxFeatures) implementation(libs.kotlin.script.runtime)
        }
    }
}

val openrndr = Openrndr()
