package com.stellarforge.composebooking.domain.repository

import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.data.model.BookedSlot
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Defines the contract for managing Appointment data within the Domain layer.
 *
 * This repository focuses specifically on the *booking transactions* and *schedule management*.
 * It isolates the UseCases from the underlying data sources (Firestore).
 */
interface AppointmentRepository {

    /**
     * Fetches a list of appointments for a specific business owner on a given date.
     * Used primarily in the Owner's Schedule screen.
     *
     * @param ownerId The ID of the business owner.
     * @param date The specific date to retrieve appointments for.
     * @return A [Result] containing the list of appointments or an error.
     */
    suspend fun getAppointmentsForDate(ownerId: String, date: LocalDate): Result<List<Appointment>>

    /**
     * Returns a real-time stream of appointments for a specific customer.
     * Used in the "My Bookings" screen to show Upcoming and Past appointments.
     *
     * @param userId The ID of the customer.
     * @return A [Flow] that emits updates whenever the user's booking list changes.
     */
    fun getMyBookingsStream(userId: String): Flow<Result<List<Appointment>>>

    /**
     * Deletes an appointment record.
     * Note: This is typically part of a larger cancellation flow that also frees up the slot.
     *
     * @param appointmentId The unique ID of the appointment to remove.
     */
    suspend fun deleteAppointment(appointmentId: String): Result<Unit>

    /**
     * Performs an atomic transaction to create an Appointment and reserve a Time Slot simultaneously.
     * This ensures data integrity: an appointment cannot exist without a slot, and vice-versa.
     *
     * @param appointment The appointment details (User, Service, Time).
     * @param slot The time slot reservation details (Start/End time).
     */
    suspend fun createAppointmentAndSlot(appointment: Appointment, slot: BookedSlot): Result<Unit>
}