import com.android.build.api.dsl.LibraryExtension
import com.android.build.gradle.BaseExtension
import jdk.internal.org.jline.utils.ExecHelper.exec
import sun.jvmstat.monitor.MonitoredVmUtil.commandLine

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("com.vanniktech.maven.publish") version "0.36.0"
}

extensions.configure<LibraryExtension>  {
    namespace = "com.apexfission.android.carddetectionlite"
    compileSdk = 36

    defaultConfig {
        minSdk = 26


        consumerProguardFiles("consumer-rules.pro")


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
        testInstrumentationRunnerArguments["useTestStorageService"] = "true"
        if (project.hasProperty("imageTestsOnly")) {
            testInstrumentationRunnerArguments["annotation"] = "com.apexfission.android.carddetectionlite.GenerateImage"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }


}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":coordinates"))

    implementation(libs.androidx.compose.material.icons.extended)

    /* -------------------- CameraX -------------------- */
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.junit.ktx)

    /* ---------------- TensorFlow Lite ---------------- */
    implementation(libs.litert.gpu)
    implementation(libs.litert.support){
        exclude(group = "com.google.ai.edge.litert", module = "litert-support-api")
    }

    /* -------------------- ExoPlayer -------------------- */
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    implementation(libs.text.recognition)
    implementation(libs.accompanist.permissions)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    androidTestImplementation(project(":tfmodel"))
    androidTestImplementation("androidx.test.services:storage:1.4.2")
    androidTestUtil("androidx.test.services:test-services:1.4.2")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    testImplementation(libs.junit)
}




tasks.register<Copy>("runTestsAndExtractImages") {
    description = "Runs UI tests, copies generated images to the project, and cleans the device."
    group = "verification"

    dependsOn("connectedDebugAndroidTest")
    from(layout.buildDirectory.dir("outputs/connected_android_test_additional_output"))
    include("**/*.png")
    includeEmptyDirs = false

    // Intercept the path and strip the first 3 folders
    // (e.g. debugAndroidTest/connected/emulator_name/)
    eachFile {
        val segments = relativePath.segments
        if (segments.size > 3) {
            // Drops the top 3 directories and joins the rest back together
            path = segments.drop(3).joinToString("/")
        }
    }

    into(layout.projectDirectory.dir("test/results/detection"))

    doLast {
        // Asks the Android Gradle Plugin for the exact path to adb.exe
        val adbPath = project.extensions.getByType<BaseExtension>().adbExecutable.absolutePath

        ProcessBuilder(adbPath, "shell", "rm", "-rf", "/sdcard/googletest/test_outputfiles/*")
            .start()
            .waitFor()

        println("Cleaned up test images from the device.")
    }
}



mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
}

mavenPublishing {
    coordinates(
        "com.apexfission.android.carddetectionlite",
        "core",
        "0.1.0-B2"
    )

    pom {
        name.set("Card Detection Lite")
        description.set("Card Detection Lite is a high-performance Android module for real-time ID detection using Sentinel-Card and TFLite. Built with Jetpack Compose and CameraX, it leverages GPU acceleration for rapid inference. Key features include an auto-cutout tool, a lock-on process, and intelligent auto-focus.")
        inceptionYear.set("2026")
        url.set("https.github.com/lambdawalker/android.card_detection_lite")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("lambdawalker")
                name.set("David Garcia")
                url.set("https://github.com/lambdawalker")
                email.set("lambdawalker@isdavid.com")
            }
        }

        scm {
            url.set("https://github.com/lambdawalker/android.card_detection_lite")
            connection.set("scm:git:git://github.com:lambdawalker/android.card_detection_lite.git")
            developerConnection.set("scm:git:ssh://git@github.com:lambdawalker/android.card_detection_lite.git")
        }
    }
}


