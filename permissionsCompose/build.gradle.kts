import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("com.vanniktech.maven.publish") version "0.36.0"
}

extensions.configure<LibraryExtension> {
    namespace = "com.apexfission.android.permissionscompose"
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
    implementation(libs.androidx.compose.material.icons.extended)


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
        "com.apexfission.android.permissionscompose",
        "core",
        "0.0.1-B0"
    )

    pom {
        name.set("Permission Compose")
        description.set("A lightweight Jetpack Compose library for handling Android runtime permissions. It provides a customizable UI component that explains the rationale for a permission request before showing the system dialog. This 'permission primer' pattern helps improve user trust and increases the permission acceptance rate by providing clear context.")

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