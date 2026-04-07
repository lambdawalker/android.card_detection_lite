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
        "sentinel-card-model",
        "TFY11640F16-0.1.0-B2"
    )

    pom {
        name.set("Sentinel Card Model")
        description.set("Sentinel Card Model is a YoloV11 based model trained with more than 20,000 examples. It is optimized for mobile inference, detects horizontal/vertical cards, photos, MRZ text, and multiple barcode formats (PDF417, QR), providing high-precision extraction for ID pipelines.")
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