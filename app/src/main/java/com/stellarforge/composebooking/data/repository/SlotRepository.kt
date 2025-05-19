package com.stellarforge.composebooking.data.repository

import com.stellarforge.composebooking.data.model.BookedSlot
import java.time.LocalDate

interface SlotRepository {
    suspend fun addSlot(slot: BookedSlot): Result<Unit>
    suspend fun getSlotsForDate(date: LocalDate): Result<List<BookedSlot>>
    suspend fun deleteSlotByAppointmentId(appointmentId: String): Result<Unit>
}