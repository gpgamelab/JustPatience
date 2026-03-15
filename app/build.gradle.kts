import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Apply the Google Services plugin to the app module
    id("com.google.gms.google-services")
    // KSP plugin for annotation processing (required for Room)
    alias(libs.plugins.ksp)
}

val useProductionAds = providers.gradleProperty("useProductionAds")
    .map(String::toBoolean)
    .orElse(false)
    .get()

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
val hasReleaseKeystore = keystorePropertiesFile.exists()

if (hasReleaseKeystore) {
    FileInputStream(keystorePropertiesFile).use { keystoreProperties.load(it) }
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
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        debug {
            resValue("string", "banner_ad_unit_id", "ca-app-pub-3940256099942544/6300978111")
            resValue("bool", "use_test_ad_ids", "true")
        }
        release {
            val releaseBannerId = if (useProductionAds) {
                "ca-app-pub-7092037186763886/6653974301"
            } else {
                "ca-app-pub-3940256099942544/6300978111"
            }
            resValue("string", "banner_ad_unit_id", releaseBannerId)
            resValue("bool", "use_test_ad_ids", (!useProductionAds).toString())
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                logger.warn("keystore.properties not found; release is currently using debug signing")
                signingConfigs.getByName("debug")
            }
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
    implementation("androidx.fragment:fragment-ktx:1.6.2") // For 'by viewModels()' in fragments

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

    // --- Room Database Dependencies ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // --- RecyclerView ---
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // If you need analytics later, uncomment this:
    // implementation("com.google.firebase:firebase-analytics")

    // --- Google Mobile Ads SDK ---
    implementation("com.google.android.gms:play-services-ads:22.6.0")

}
