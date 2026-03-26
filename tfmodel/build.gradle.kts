import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)

    id("com.vanniktech.maven.publish") version "0.36.0"
}

extensions.configure<LibraryExtension> {
    namespace = "com.apexfission.android.carddetectionlite.tfmodel"
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
    androidResources {
        noCompress += setOf("tflite")
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":cardDetectionLite"))

    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
}

mavenPublishing {
    coordinates(
        "com.apexfission.android.carddetectionlite",
        "tfmodel-Y11-640-F16",
        "0.0.1-A1"
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