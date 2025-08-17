package com.stellarforge.composebooking.data.repository

import com.stellarforge.composebooking.data.model.BookedSlot
import com.stellarforge.composebooking.utils.Result
import java.time.LocalDate

interface SlotRepository {
    suspend fun addSlot(slot: BookedSlot): Result<Unit>
    suspend fun getSlotsForDate(ownerId: String, date: LocalDate): Result<List<BookedSlot>>
    suspend fun deleteSlotByAppointmentId(appointmentId: String): Result<Unit>
}