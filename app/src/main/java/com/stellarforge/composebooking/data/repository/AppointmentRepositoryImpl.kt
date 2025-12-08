package com.stellarforge.composebooking.data.repository

import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.data.model.BookedSlot
import com.stellarforge.composebooking.data.remote.AppointmentRemoteDataSource
import com.stellarforge.composebooking.utils.Result
import com.stellarforge.composebooking.di.IoDispatcher
import com.stellarforge.composebooking.domain.repository.AppointmentRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

/**
 * Concrete implementation of [AppointmentRepository] using Cloud Firestore via [AppointmentRemoteDataSource].
 *
 * Key Architecture Decisions:
 * - **Dispatcher Management:** All database operations are offloaded to the [IoDispatcher] to ensure the Main Thread remains free.
 * - **Flow Handling:** Uses `flowOn` to ensure the reactive stream operates on the correct thread.
 * - **Transaction Delegation:** Delegates the complex batch write logic to the DataSource.
 */
class AppointmentRepositoryImpl @Inject constructor(
    private val appointmentDataSource: AppointmentRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AppointmentRepository {

    /**
     * Fetches appointments for a specific date (used in Owner Schedule).
     * Switches to IO context for network operation.
     */
    override suspend fun getAppointmentsForDate(
        ownerId: String,
        date: LocalDate
    ): Result<List<Appointment>> {
        return withContext(ioDispatcher) {
            appointmentDataSource.getAppointmentsForDate(ownerId, date)
        }
    }

    /**
     * Provides a real-time stream of the current user's bookings.
     * Note: `flowOn(ioDispatcher)` ensures the Firestore listener runs on the IO thread.
     */
    override fun getMyBookingsStream(userId: String): Flow<Result<List<Appointment>>> {
        return appointmentDataSource.getMyBookingsStream(userId).flowOn(ioDispatcher)
    }

    /**
     * Deletes a specific appointment.
     */
    override suspend fun deleteAppointment(appointmentId: String): Result<Unit> {
        return withContext(ioDispatcher) {
            appointmentDataSource.deleteAppointment(appointmentId)
        }
    }

    /**
     * Creates an appointment and reserves the time slot atomically.
     */
    override suspend fun createAppointmentAndSlot(
        appointment: Appointment,
        slot: BookedSlot
    ): Result<Unit> {
        return withContext(ioDispatcher) {
            appointmentDataSource.createAppointmentAndSlot(appointment, slot)
        }
    }
}