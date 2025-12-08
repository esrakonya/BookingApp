package com.stellarforge.composebooking.data.remote

import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.utils.Result
import com.stellarforge.composebooking.data.model.BookedSlot
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Interface defining the data source operations for appointment data.
 */
interface AppointmentRemoteDataSource {
    suspend fun getAppointmentsForDate(ownerId: String, date: LocalDate): Result<List<Appointment>>

    fun getMyBookingsStream(userId: String): Flow<Result<List<Appointment>>>

    suspend fun deleteAppointment(appointmentId: String): Result<Unit>

    suspend fun createAppointmentAndSlot(appointment: Appointment, slot: BookedSlot): Result<Unit>
}
