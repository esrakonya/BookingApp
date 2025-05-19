package com.stellarforge.composebooking.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {

    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    // Main dispatcher genellikle doğrudan enjekte edilmez,
    // viewModelScope gibi yapılar onu kullanır veya testlerde Rule ile değiştirilir.
    // Ama gerekirse sağlanabilir:
    // @Provides
    // @Singleton
    // @MainDispatcher
    // fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
