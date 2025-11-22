package com.stellarforge.composebooking.di

import com.stellarforge.composebooking.data.remote.*
import com.stellarforge.composebooking.data.repository.*
import com.stellarforge.composebooking.domain.repository.AppointmentRepository
import com.stellarforge.composebooking.domain.repository.AuthRepository
import com.stellarforge.composebooking.domain.repository.BusinessProfileRepository
import com.stellarforge.composebooking.domain.repository.ServiceRepository
import com.stellarforge.composebooking.domain.repository.SlotRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    // --- DataSource Binds ---
    @Binds
    @Singleton
    abstract fun bindAuthRemoteDataSource(
        authRemoteDataSourceImpl: AuthRemoteDataSourceImpl
    ) : AuthRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindBusinessProfileRemoteDataSource(
        businessProfileRemoteDataSourceImpl: BusinessProfileRemoteDataSourceImpl
    ) : BusinessProfileRemoteDataSource


    @Binds
    @Singleton
    abstract fun bindAppointmentRemoteDataSource(
        appointmentRemoteDataSourceImpl: AppointmentRemoteDataSourceImpl
    ): AppointmentRemoteDataSource


    @Binds
    @Singleton
    abstract fun bindSlotRemoteDataSource(
        slotRemoteDataSourceImpl: SlotRemoteDataSourceImpl
    ): SlotRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindServiceRemoteDataSource(
        serviceRemoteDataSourceImpl: ServiceRemoteDataSourceImpl
    ): ServiceRemoteDataSource

    // --- Repository Binds ---
    @Binds
    @Singleton
    abstract fun bindAppointmentRepository(
        appointmentRepositoryImpl: AppointmentRepositoryImpl
    ) : AppointmentRepository

    @Binds
    @Singleton
    abstract fun bindServiceRepository(
        serviceRepositoryImpl: ServiceRepositoryImpl
    ) : ServiceRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ) : AuthRepository

    @Binds
    @Singleton
    abstract fun bindSlotRepository(
        slotRepositoryImpl: SlotRepositoryImpl
    ) : SlotRepository

    @Binds
    @Singleton
    abstract fun bindBusinessProfileRepository(
        businessProfileRepositoryImpl: BusinessProfileRepositoryImpl
    ) : BusinessProfileRepository
}