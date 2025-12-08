package com.stellarforge.composebooking

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Base Application class for the project.
 *
 * **Role:**
 * - Triggers Hilt's code generation via [@HiltAndroidApp], creating the Dependency Injection container.
 * - Initializes application-wide libraries like Timber for logging.
 */
@HiltAndroidApp
class StellarForgeBookingApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Logging ONLY in Debug builds.
        // This prevents sensitive logs from appearing in Production releases.
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}