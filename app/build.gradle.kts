plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.danielmccullum.hrzones"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.danielmccullum.hrzones"
        minSdk = 23
        targetSdk = 34
        versionCode = 3
        versionName = "1.2"
    }

    // Release keystore lives outside the repo; its path and passwords come from ~/.gradle/gradle.properties.
    // Without them (a fresh checkout, CI) release builds fall back to the debug key.
    val releaseStoreFile = providers.gradleProperty("hrzones.storeFile").orNull
    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = providers.gradleProperty("hrzones.storePassword").get()
                keyAlias = providers.gradleProperty("hrzones.keyAlias").get()
                keyPassword = providers.gradleProperty("hrzones.keyPassword").get()
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("io.hammerhead:karoo-ext:1.1.9")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
}
