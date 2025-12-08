package com.stellarforge.composebooking.domain.repository

import com.stellarforge.composebooking.data.model.BookedSlot
import com.stellarforge.composebooking.utils.Result
import java.time.LocalDate

/**
 * Defines the contract for managing "Booked Slots" (Occupied Time Intervals) within the Domain layer.
 *
 * **Architecture & Performance Note:**
 * In this system, `BookedSlot` is a lightweight entity distinct from `Appointment`.
 * While an `Appointment` contains heavy data (User info, Service details, Price),
 * a `BookedSlot` only contains the time range (`startTime`, `endTime`) and the `ownerId`.
 *
 * This separation allows the application to query availability for a specific date
 * extremely fast, without downloading unnecessary sensitive customer data.
 */
interface SlotRepository {

    /**
     * Persists a new occupied time slot.
     *
     * This is typically called within an atomic transaction alongside creating an Appointment.
     *
     * @param slot The time slot details to save.
     */
    suspend fun addSlot(slot: BookedSlot): Result<Unit>

    /**
     * Retrieves all occupied time slots for a specific business on a specific date.
     *
     * This data is consumed by the Availability Logic (UseCase) to calculate free time gaps.
     *
     * @param ownerId The unique ID of the business owner.
     * @param date The date to query.
     * @return A list of [BookedSlot] objects representing busy times.
     */
    suspend fun getSlotsForDate(ownerId: String, date: LocalDate): Result<List<BookedSlot>>

    /**
     * Removes the occupied time slot associated with a specific appointment.
     *
     * This effectively "frees up" the time in the calendar when a booking is cancelled.
     *
     * @param appointmentId The unique ID of the appointment that is being cancelled.
     */
    suspend fun deleteSlotByAppointmentId(appointmentId: String): Result<Unit>
}