package com.stellarforge.composebooking.di

import com.stellarforge.composebooking.data.remote.AuthRemoteDataSource
import com.stellarforge.composebooking.data.remote.AuthRemoteDataSourceImpl
import com.stellarforge.composebooking.data.remote.BusinessProfileRemoteDataSource
import com.stellarforge.composebooking.data.remote.BusinessProfileRemoteDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {

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

    // Gelecekte eklenecek diğer DataSource binding'leri:
    /*
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
    */
}