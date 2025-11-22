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
        }
    }
}