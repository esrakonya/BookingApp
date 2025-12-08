package com.stellarforge.composebooking.data.repository

import com.stellarforge.composebooking.data.model.BookedSlot
import com.stellarforge.composebooking.data.remote.SlotRemoteDataSource
import com.stellarforge.composebooking.di.IoDispatcher
import com.stellarforge.composebooking.domain.repository.SlotRepository
import kotlinx.coroutines.CoroutineDispatcher
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

/**
 * Concrete implementation of [SlotRepository].
 *
 * This repository is responsible for managing "Time Slots".
 * Unlike [AppointmentRepository] which handles full customer details, this repository
 * focuses purely on **Time Availability**.
 *
 * **Performance Note:**
 * Keeping Slots separate from Appointments allows the app to quickly query availability
 * for a whole month without downloading heavy customer data.
 */
class SlotRepositoryImpl @Inject constructor(
    private val slotRemoteDataSource: SlotRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SlotRepository {

    /**
     * Persists a new occupied slot.
     * Typically called when a booking is confirmed.
     */
    override suspend fun addSlot(slot: BookedSlot): Result<Unit> {
        return withContext(ioDispatcher) {
            slotRemoteDataSource.addSlot(slot)
        }
    }

    /**
     * Fetches all booked slots for a specific date.
     * Used by the [GetAvailableSlotsUseCase] to calculate free time intervals.
     */
    override suspend fun getSlotsForDate(ownerId: String, date: LocalDate): Result<List<BookedSlot>> {
        return withContext(ioDispatcher) {
            slotRemoteDataSource.getSlotsForDate(ownerId, date)
        }
    }

    /**
     * Frees up a time slot when an appointment is cancelled.
     */
    override suspend fun deleteSlotByAppointmentId(appointmentId: String): Result<Unit> {
        return withContext(ioDispatcher) {
            slotRemoteDataSource.deleteSlotByAppointmentId(appointmentId)
        }
    }
}