package com.stellarforge.composebooking.di

import com.stellarforge.composebooking.data.repository.AppointmentRepository
import com.stellarforge.composebooking.data.repository.AppointmentRepositoryImpl
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.stellarforge.composebooking.data.repository.AuthRepository
import com.stellarforge.composebooking.data.repository.AuthRepositoryImpl
import com.stellarforge.composebooking.data.repository.BusinessProfileRepository
import com.stellarforge.composebooking.data.repository.BusinessProfileRepositoryImpl
import com.stellarforge.composebooking.data.repository.SlotRepository
import com.stellarforge.composebooking.data.repository.SlotRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // AppointmentRepository arayüzü istendiğinde AppointmentRepositoryImpl örneğini sağla
    @Binds
    @Singleton
    abstract fun bindAppointmentRepository(
        appointmentRepositoryImpl: AppointmentRepositoryImpl
    ) : AppointmentRepository

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

