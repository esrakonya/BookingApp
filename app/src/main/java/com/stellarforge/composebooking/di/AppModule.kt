package com.stellarforge.composebooking.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * Hilt Module for providing external dependencies (Third-party libraries, System services).
 *
 * **Role:**
 * Unlike [DataModule] which binds interfaces to implementations, this module uses [@Provides]
 * to create instances of classes we do not own (e.g., Firebase, CoroutineDispatchers).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // --- Firebase Providers ---

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

    // --- Coroutine Dispatcher Providers ---
    // Injecting dispatchers makes testing easier (we can swap them with TestDispatchers).

    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}