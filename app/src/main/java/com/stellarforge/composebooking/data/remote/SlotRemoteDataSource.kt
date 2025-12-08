package com.stellarforge.composebooking.data.remote

import com.stellarforge.composebooking.data.model.BookedSlot
import com.stellarforge.composebooking.utils.Result
import java.time.LocalDate

/**
 * Defines the contract for raw data source operations related to [BookedSlot] data.
 *
 * **Architecture Note:**
 * In this system, 'Slots' are lightweight records representing occupied time ranges (Start Time - End Time).
 * They are queried separately from full 'Appointment' details. This allows the app to
 * efficiently calculate daily availability without downloading heavy customer data (Names, Phones, etc.).
 */
interface SlotRemoteDataSource {

    /**
     * Persists a new occupied time slot to the remote database.
     * Usually called within a transaction alongside appointment creation.
     */
    suspend fun addSlot(slot: BookedSlot): Result<Unit>

    /**
     * Fetches all occupied slots for a specific business owner on a specific date.
     * Used to filter out unavailable times in the booking calendar.
     *
     * @param ownerId The business owner's unique ID.
     * @param date The date to query.
     */
    suspend fun getSlotsForDate(ownerId: String, date: LocalDate): Result<List<BookedSlot>>

    /**
     * Removes occupied slots associated with a specific appointment ID.
     * Called when an appointment is cancelled to free up the time.
     */
    suspend fun deleteSlotByAppointmentId(appointmentId: String): Result<Unit>
}