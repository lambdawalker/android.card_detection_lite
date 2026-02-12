plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.apexfission.android.tflitetest"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.apexfission.android.tflitetest"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }

}

dependencies {
    /* -------------------- CameraX -------------------- */
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    /* ---------------- TensorFlow Lite ---------------- */
    implementation("org.tensorflow:tensorflow-lite:2.16.1")

    // Optional but recommended if you later add GPU
    implementation("org.tensorflow:tensorflow-lite-gpu:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("com.google.android.gms:play-services-tflite-gpu:16.4.0") // check latest


    //OCR
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // For viewModel() delegate
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
// For collectAsStateWithLifecycle()
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}