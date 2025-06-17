package com.stellarforge.composebooking.data.repository

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.data.remote.AppointmentRemoteDataSource
import com.stellarforge.composebooking.data.remote.ServiceRemoteDataSource
import com.stellarforge.composebooking.utils.Result
import com.stellarforge.composebooking.di.IoDispatcher
import com.stellarforge.composebooking.utils.FirebaseConstants
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppointmentRepositoryImpl @Inject constructor(
    private val serviceDataSource: ServiceRemoteDataSource,
    private val appointmentDataSource: AppointmentRemoteDataSource,
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AppointmentRepository {
    override fun getServices(): Flow<Result<List<Service>>> {
        return serviceDataSource.getServicesFlow()
            .catch { e ->
                Timber.e(e, "Error fetching services flow in Repository")
                emit(Result.Error(Exception("Repository: Error fetching services", e)))
            }.flowOn(ioDispatcher)
    }

    override suspend fun getAppointmentsForDate(date: LocalDate): Result<List<Appointment>> {
        return withContext(ioDispatcher) {
            appointmentDataSource.getAppointmentsForDate(date)
        }
    }

    override suspend fun createAppointment(appointment: Appointment): Result<Unit> {
        return withContext(ioDispatcher) {
            appointmentDataSource.createAppointment(appointment)
        }
    }

    override suspend fun getServiceDetails(serviceId: String): Result<Service> {
        return withContext(ioDispatcher) {
            serviceDataSource.getServiceDetails(serviceId)
        }
    }

    override fun getNewAppointmentReference(): DocumentReference {
        return firestore.collection(FirebaseConstants.APPOINTMENTS_COLLECTION).document()
    }

    override suspend fun createAppointmentWithId(
        ref: DocumentReference,
        appointment: Appointment
    ): Result<Unit> {
        return withContext(ioDispatcher) {
            appointmentDataSource.createAppointmentWithId(ref, appointment)
        }
    }

}