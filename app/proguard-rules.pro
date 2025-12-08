# ----------------------------------------------------------
# 1. GENERAL SETTINGS & DEBUGGING
# ----------------------------------------------------------
# Essential for preserving stack trace information during crashes.
# If removed, crash logs will display "Unknown Source" instead of
# file names and line numbers, making debugging impossible.
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes EnclosingMethod

# ----------------------------------------------------------
# 2. FIREBASE & FIRESTORE (CRITICAL)
# ----------------------------------------------------------
# Prevents obfuscation of Data Models.
# Firestore uses reflection to map database documents to Kotlin objects.
# If these class names or fields are renamed by R8, data binding will fail.
-keep class com.stellarforge.composebooking.data.model.** { *; }

# ----------------------------------------------------------
# 3. HILT & DAGGER (Dependency Injection)
# ----------------------------------------------------------
# Standard rules for Hilt to generate dependency graphs correctly.
-keep class dagger.hilt.** { *; }
-keep interface dagger.hilt.** { *; }
-keep public class * extends dagger.hilt.android.HiltAndroidApp

# Your Custom Application Class.
# IMPORTANT: Ensure this matches the 'android:name' attribute in AndroidManifest.xml.
-keep class com.stellarforge.composebooking.StellarForgeBookingApp { *; }

# ----------------------------------------------------------
# 4. JETPACK COMPOSE
# ----------------------------------------------------------
# Required rules to ensure Jetpack Compose runtime functions correctly with R8.
-keepattributes InnerClasses
-keep class androidx.compose.** { *; }

# ----------------------------------------------------------
# 5. THIRD-PARTY LIBRARIES (Calendar, Coil, etc.)
# ----------------------------------------------------------
# Rules for the Kizitonwose Calendar library to prevent runtime warnings/errors.
-keep class com.kizitonwose.calendar.** { *; }
-dontwarn com.kizitonwose.calendar.**