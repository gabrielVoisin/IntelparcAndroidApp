plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.drautomation.intelpark"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.drautomation.intelpark"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"
        // Address of the server on the Raspberry Pi; can be changed in the app (Welcome screen > gear icon)
        buildConfigField("String", "DEFAULT_SERVER_URL", "\"http://192.168.1.50\"")
    }

    // Fixed debug key, committed on purpose: every build (CI or local) has the same signature,
    // so a new APK installs over the previous one without uninstalling. Prototype only.
    signingConfigs {
        getByName("debug") {
            storeFile = file("intelpark-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            // Prototype: no shrinking (simpler to debug). Enable it with tested ProGuard rules before publishing.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.mlkit)
    implementation(libs.mlkit.barcode)
    implementation(libs.mlkit.text)
    implementation(libs.osmdroid)
    implementation(libs.play.services.location)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
