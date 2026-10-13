// The documented minimums: AGP 8.6, Kotlin 2.0, compileSdk 35, Java 11.
plugins {
    id("com.android.application") version "8.6.0"
    id("org.jetbrains.kotlin.android") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
}

android {
    namespace = "com.siliconcircuits.toaststack.consumer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.siliconcircuits.toaststack.consumer"
        minSdk = 24
        targetSdk = 35
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // The newest version in Maven Local, which CI has just published.
    implementation("com.siliconcircuits:toaststack:+")
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.10.1")
}
