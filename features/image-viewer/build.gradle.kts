plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ae.image_viewer"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    aaptOptions {
        noCompress("tflite")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.material3.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    implementation(project(":core:design-system"))

    // Compose
    implementation(platform(libs.androidx.compose.bom.v20250601))
    implementation(libs.ui)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.compose.ui.ui2)
    implementation(libs.runtime)
    implementation(libs.androidx.compose.foundation.foundation)
    implementation(libs.androidx.activity.compose)


    // Coil
    implementation(libs.coil.compose)

    // TensorFlow Lite
    implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite)

    // Optional: GPU delegate
    implementation(libs.tensorflow.lite.gpu)
    implementation(libs.tensorflow.lite.gpu.delegate.plugin)

    // ML Kit Face Detection
    implementation(libs.face.detection)

    // ML Kit for image labeling
    implementation(libs.image.labeling)
    implementation(libs.image.labeling.custom)

    implementation(libs.tasks.vision)
    implementation(libs.tasks.core)


    // Coroutines support for Play Services
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.coroutines.android)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ui.test.junit4)

}