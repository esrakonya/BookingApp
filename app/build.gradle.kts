// app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt) // Kapt kullanıyoruz
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.compose.compiler) // Compose Compiler Plugin'i uygula
}

android {
    namespace = "com.stellarforge.composebooking"
    compileSdk = 35 // Gerekli en düşük SDK

    defaultConfig {
        applicationId = "com.stellarforge.composebooking"
        minSdk = 21
        targetSdk = 34 // compileSdk ile aynı olması önerilir
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Başlangıç için
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true // java.time için
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Library Desugaring
    coreLibraryDesugaring(libs.android.desugarJdkLibs)

    // Temel AndroidX & Lifecycle
    implementation(libs.androidx.core.ktx) // Artık doğru sürümü kullanmalı
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Compose (BOM ve Bundle)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)

    // Coroutines (Bundle)
    implementation(libs.bundles.coroutines)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Firebase (BOM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)

    // Coil
    implementation(libs.coil.compose)

    // Javax Inject
    implementation(libs.javax.inject)

    // Test Bağımlılıkları
    testImplementation(libs.junit)
    testImplementation(libs.bundles.unitTest)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.bundles.androidTest) // Alias düzeltildi
    debugImplementation(libs.bundles.debug)

    implementation(libs.timber)
}

// Kapt konfigürasyonu
kapt {
    correctErrorTypes = true
}