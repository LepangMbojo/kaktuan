import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
}

val properties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    properties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.example.kaktuan"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.kaktuan"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val apiKey = properties.getProperty("VISION_API_KEY") ?: ""
        buildConfigField("String", "VISION_API_KEY", "\"$apiKey\"")

        val geminiKey = properties.getProperty("GEMINI_API_KEY") ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")

        val supabaseUrl = properties.getProperty("SUPABASE_URL") ?: ""
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")

        val supabaseKey = properties.getProperty("SUPABASE_KEY") ?: ""
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")

        val googleClientId = properties.getProperty("GOOGLE_WEB_CLIENT_ID") ?: ""
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleClientId\"")
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
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    // --- 1. LIBRARY BAWAAN ANDROID ---
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation(libs.androidx.activity)

    // --- 2. LIBRARY CAMERAX (Untuk Scanner) ---
    val cameraxVersion = "1.3.0"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // --- 3. LIBRARY API & NETWORK ---
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // --- 4. MEDIA & UI ---
    implementation("com.github.bumptech.glide:glide:4.16.0") // Memuat gambar

    // --- 5. SUPABASE (Pengganti Firebase) ---
    val supabaseVersion = "2.0.0"
    implementation("io.github.jan-tennert.supabase:postgrest-kt:$supabaseVersion") // Database
    implementation("io.github.jan-tennert.supabase:gotrue-kt:$supabaseVersion") // Auth
    implementation("io.github.jan-tennert.supabase:storage-kt:$supabaseVersion") // Storage

    val ktorVersion = "2.3.0"
    implementation("io.ktor:ktor-client-android:$ktorVersion")

    // Google Sign-In (Dibutuhkan untuk login Google)
    implementation("com.google.android.gms:play-services-auth:20.7.0")
}