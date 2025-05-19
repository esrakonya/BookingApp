package com.stellarforge.composebooking.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.stellarforge.composebooking.utils.FirebaseConstants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        return Firebase.firestore
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object AppModule { // Veya FirebaseModule
        @Provides
        @Singleton
        fun provideFirebaseConstants(): FirebaseConstants {
            return FirebaseConstants // Object olduğu için direkt döndür
        }
    }
}