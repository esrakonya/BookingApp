package com.stellarforge.composebooking

import android.app.Application
import dagger.hilt.android.HiltAndroidApp // Bu import önemli
import timber.log.Timber
import com.stellarforge.composebooking.BuildConfig

@HiltAndroidApp
class StellarForgeBookingApp : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.d("Timber initialized for DEBUG build.")
        } else {
            // Release mode: No Timber.DebugTree planted. Plant a custom tree for crash reporting if needed.
            Timber.i("Timber not initialized for RELEASE build or custom tree planted.")
        }
    }
}