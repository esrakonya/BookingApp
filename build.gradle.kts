// Top-level build.gradle.kts (Project Level)

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.compose.compiler) apply false // Compose Compiler Plugin
}