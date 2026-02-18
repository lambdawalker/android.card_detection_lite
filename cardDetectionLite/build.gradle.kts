plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("com.vanniktech.maven.publish") version "0.36.0"
}

android {
    androidResources {
        noCompress += listOf("tflite")
    }
    namespace = "com.apexfission.android.carddetectionlite"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    /* -------------------- CameraX -------------------- */
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    /* ---------------- TensorFlow Lite ---------------- */
    implementation(libs.litert.gpu)
    implementation(libs.litert.support){
        exclude(group = "com.google.ai.edge.litert", module = "litert-support-api")
    }

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

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    testImplementation(libs.junit)
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
}

mavenPublishing {
    coordinates(
        "com.apexfission.android.carddetectionlite",
        "card-detection-lite",
        "0.0.8"
    )

    pom {
        name.set("Card Detection Lite")
        description.set("Card Detection Lite is a high-performance Android module that provides developers with a ready-to-use solution for real-time ID card detection and extraction using YOLO models and TensorFlow Lite. Built with Jetpack Compose and CameraX, the module features an optimized inference pipeline with GPU acceleration that handles complex coordinate mapping from the camera's cropRect to a dynamic UI overlay. It is designed for seamless integration into existing apps, offering customizable bounding box visualization, class labeling, and a robust \"cutout\" feature that automatically captures and crops high-resolution images of detected ID cards for further processing or identity verification workflows.")
        inceptionYear.set("2026")
        url.set("https://github.com/lambdawalker/android.card_detection_lite")

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