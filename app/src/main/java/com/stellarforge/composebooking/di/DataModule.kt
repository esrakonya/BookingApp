package com.stellarforge.composebooking.di

import com.stellarforge.composebooking.data.remote.*
import com.stellarforge.composebooking.data.repository.*
import com.stellarforge.composebooking.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module for binding Interfaces to their Concrete Implementations.
 *
 * **Role:**
 * Uses [@Binds] to tell Hilt: "When a class asks for [AuthRepository], give them [AuthRepositoryImpl]".
 * This is efficient because it doesn't generate reflection code like [@Provides].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    // =================================================================
    // Remote Data Sources (Low Level)
    // =================================================================

    @Binds
    @Singleton
    abstract fun bindAuthRemoteDataSource(
        impl: AuthRemoteDataSourceImpl
    ): AuthRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindBusinessProfileRemoteDataSource(
        impl: BusinessProfileRemoteDataSourceImpl
    ): BusinessProfileRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindAppointmentRemoteDataSource(
        impl: AppointmentRemoteDataSourceImpl
    ): AppointmentRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindSlotRemoteDataSource(
        impl: SlotRemoteDataSourceImpl
    ): SlotRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindServiceRemoteDataSource(
        impl: ServiceRemoteDataSourceImpl
    ): ServiceRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindCustomerProfileRemoteDataSource(
        impl: CustomerProfileRemoteDataSourceImpl
    ): CustomerProfileRemoteDataSource

    // =================================================================
    // Repositories (Domain Level)
    // =================================================================

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindBusinessProfileRepository(
        impl: BusinessProfileRepositoryImpl
    ): BusinessProfileRepository

    @Binds
    @Singleton
    abstract fun bindAppointmentRepository(
        impl: AppointmentRepositoryImpl
    ): AppointmentRepository

    @Binds
    @Singleton
    abstract fun bindSlotRepository(
        impl: SlotRepositoryImpl
    ): SlotRepository

    @Binds
    @Singleton
    abstract fun bindServiceRepository(
        impl: ServiceRepositoryImpl
    ): ServiceRepository

    @Binds
    @Singleton
    abstract fun bindCustomerProfileRepository(
        impl: CustomerProfileRepositoryImpl
    ): CustomerProfileRepository
}