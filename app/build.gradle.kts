plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Apply the Google Services plugin to the app module
    id("com.google.gms.google-services")
}

android {
    namespace = "com.gpgamelab.justpatience"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.gpgamelab.justpatience"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Retrofit for making HTTP requests
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    // Gson converter for turning JSON into Kotlin data classes (ApiModels)
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Optional: Coroutines support for asynchronous operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // DataStore for secure token persistence (recommended over SharedPreferences)
    // NOTE: Use the latest stable version if 1.0.0 causes issues.
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // --- Required for ViewModel and Lifecycle ---
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2") // For repeatOnLifecycle
    implementation("androidx.activity:activity-ktx:1.8.2") // For 'by viewModels()'
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")

    // --- Required for TokenManager (DataStore) ---
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // --- Recommended: OkHttp Logging Interceptor for network debugging ---
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Add the Firebase BOM (Bill of Materials) - Corrected syntax
    // This ensures all your Firebase libraries are compatible
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))

    // Firebase dependencies (versions are managed by the BOM)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore") // <-- NEW: Required for GameRepository

    // If you need analytics later, uncomment this:
    // implementation("com.google.firebase:firebase-analytics")

}
