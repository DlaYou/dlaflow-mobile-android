plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "pl.dlaflow.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "pl.dlaflow.mobile"
        minSdk = 28
        targetSdk = 35
        versionCode = 4
        versionName = "0.3.0"
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
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    val composeBom = platform("androidx.compose:compose-bom:2025.12.00")

    implementation("androidx.activity:activity:1.8.2")
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(composeBom)
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    testImplementation("junit:junit:4.13.2")
    debugImplementation(composeBom)
    debugImplementation("androidx.compose.ui:ui-tooling")
}
