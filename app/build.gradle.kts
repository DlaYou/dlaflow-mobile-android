plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "pl.dlaflow.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "pl.dlaflow.mobile"
        minSdk = 28
        targetSdk = 35
        versionCode = 2
        versionName = "0.1.1"
    }

    val releaseStoreFile = System.getenv("ANDROID_SIGNING_STORE_FILE")
    val releaseStorePassword = System.getenv("ANDROID_SIGNING_STORE_PASSWORD")
    val releaseKeyAlias = System.getenv("ANDROID_SIGNING_KEY_ALIAS")
    val releaseKeyPassword = System.getenv("ANDROID_SIGNING_KEY_PASSWORD")
    val hasReleaseSigning = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { !it.isNullOrBlank() }

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}
