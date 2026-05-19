import java.util.Properties // Wajib tambahkan ini di baris paling atas

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

// Tambahkan blok ini untuk membaca file local.properties
val properties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    properties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.example.kaktuan"
    compileSdk = 36 // Catatan: Saya sarankan pakai 34 atau 35 yang stabil, 36 masih eksperimental

    // 1. Aktifkan fitur BuildConfig di sini
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.kaktuan"
        minSdk = 24
        targetSdk = 36 // Sesuaikan dengan compileSdk
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 2. Suntikkan API Key dari local.properties ke dalam BuildConfig
        val apiKey = properties.getProperty("VISION_API_KEY") ?: ""
        buildConfigField("String", "VISION_API_KEY", "\"$apiKey\"")
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // --- 1. LIBRARY BAWAAN ANDROID (Yang sempat hilang) ---
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation(libs.androidx.activity)

    // --- 2. LIBRARY CAMERAX ---
    val camerax_version = "1.3.0"
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    implementation("androidx.camera:camera-view:${camerax_version}")

    // --- 3. LIBRARY RETROFIT (Untuk Cloud Vision API) ---
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Firebase BoM — satu versi untuk semua library Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth")

    // Cloud Firestore
    implementation("com.google.firebase:firebase-firestore")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")
}