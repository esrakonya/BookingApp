package com.stellarforge.composebooking.data.repository

import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.data.model.BookedSlot
import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.data.remote.AppointmentRemoteDataSource
import com.stellarforge.composebooking.data.remote.ServiceRemoteDataSource
import com.stellarforge.composebooking.utils.Result
import com.stellarforge.composebooking.di.IoDispatcher
import com.stellarforge.composebooking.domain.repository.AppointmentRepository
import com.stellarforge.composebooking.utils.FirebaseConstants
import com.stellarforge.composebooking.utils.mapOnSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

class AppointmentRepositoryImpl @Inject constructor(
    private val serviceDataSource: ServiceRemoteDataSource,
    private val appointmentDataSource: AppointmentRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AppointmentRepository {
    override fun getServices(): Flow<Result<List<Service>>> {
        val targetOwnerId = FirebaseConstants.TARGET_BUSINESS_OWNER_ID
        return serviceDataSource.getOwnerServicesStream(targetOwnerId)
            .map { result ->
                result.mapOnSuccess { serviceList ->
                    serviceList.filter { it.isActive }
                }
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun getServiceDetails(serviceId: String): Result<Service> {
        return withContext(ioDispatcher) {
            // DataSource Result<Service> döndürdüğü için doğrudan kullanabiliriz
            serviceDataSource.getServiceDetails(serviceId)
        }
    }

    override fun getOwnerServicesStream(ownerId: String): Flow<Result<List<Service>>> {
        return serviceDataSource.getOwnerServicesStream(ownerId).flowOn(ioDispatcher)
    }

    override suspend fun addService(service: Service): Result<Unit> {
        return withContext(ioDispatcher) {
            serviceDataSource.addService(service)
        }
    }

    override suspend fun updateService(service: Service): Result<Unit> {
        return withContext(ioDispatcher) {
            serviceDataSource.updateService(service)
        }
    }

    override suspend fun deleteService(serviceId: String): Result<Unit> {
        return withContext(ioDispatcher) {
            serviceDataSource.deleteService(serviceId)
        }
    }

    override suspend fun getAppointmentsForDate(
        ownerId: String,
        date: LocalDate
    ): Result<List<Appointment>> {
        return withContext(ioDispatcher) {
            appointmentDataSource.getAppointmentsForDate(ownerId, date)
        }
    }

    override fun getMyBookingsStream(userId: String): Flow<Result<List<Appointment>>> {
        return appointmentDataSource.getMyBookingsStream(userId).flowOn(ioDispatcher)
    }

    override suspend fun deleteAppointment(appointmentId: String): Result<Unit> {
        return withContext(ioDispatcher) {
            appointmentDataSource.deleteAppointment(appointmentId)
        }
    }

    override suspend fun createAppointmentAndSlot(
        appointment: Appointment,
        slot: BookedSlot
    ): Result<Unit> {
        return withContext(ioDispatcher) {
            appointmentDataSource.createAppointmentAndSlot(appointment, slot)
        }
    }

}

/*
@Singleton
class AppointmentRepositoryImpl @Inject constructor(
    private val serviceDataSource: ServiceRemoteDataSource,
    private val appointmentDataSource: AppointmentRemoteDataSource,
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AppointmentRepository {
    override fun getServices(): Flow<Result<List<Service>>> {
        // 1. Hedef işletmenin ID'sini al.
        val targetOwnerId = FirebaseConstants.TARGET_BUSINESS_OWNER_ID

        // 2. DataSource'dan gelen ve zaten Result içeren akışı dinle.
        return serviceDataSource.getOwnerServicesStream(targetOwnerId)
            .map { result ->
                // 3. Gelen Result başarılı ise (Success), içindeki listeyi filtrele.
                //    Başarısız ise (Error), ona dokunmadan aynen geçir.
                result.mapOnSuccess { serviceList ->
                    serviceList.filter { it.isActive }
                }
            }
            .flowOn(ioDispatcher) // 4. Tüm akışın belirtilen dispatcher'da çalışmasını sağla.
    }

    override suspend fun getAppointmentsForDate(ownerId: String, date: LocalDate): Result<List<Appointment>> {
        return withContext(ioDispatcher) {
            appointmentDataSource.getAppointmentsForDate(ownerId, date)
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

    override fun getOwnerServicesStream(ownerId: String): Flow<Result<List<Service>>> {
        return serviceDataSource.getOwnerServicesStream(ownerId).flowOn(ioDispatcher)
    }

    override suspend fun addService(service: Service): Result<Unit> {
        return withContext(ioDispatcher){
            serviceDataSource.addService(service)
        }
    }

    override suspend fun updateService(service: Service): Result<Unit> {
        return withContext(ioDispatcher) {
            serviceDataSource.updateService(service)
        }
    }

    override suspend fun deleteService(serviceId: String): Result<Unit> {
        return withContext(ioDispatcher) {
            serviceDataSource.deleteService(serviceId)
        }
    }

    override fun getMyBookingsStream(userId: String): Flow<Result<List<Appointment>>> {
        return appointmentDataSource.getMyBookingsStream(userId).flowOn(ioDispatcher)
    }

    override suspend fun deleteAppointment(appointmentId: String): Result<Unit> {
        return withContext(ioDispatcher) {
            appointmentDataSource.deleteAppointment(appointmentId)
        }
    }

}
 */