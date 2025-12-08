package com.stellarforge.composebooking.di

import javax.inject.Qualifier

/**
 * Custom Qualifiers for Dependency Injection.
 *
 * **Purpose:**
 * Since [CoroutineDispatcher] is a common type, Hilt needs to know WHICH dispatcher
 * we are asking for (IO, Default, or Main). These annotations resolve that ambiguity.
 */

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class IoDispatcher

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class DefaultDispatcher

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class MainDispatcher