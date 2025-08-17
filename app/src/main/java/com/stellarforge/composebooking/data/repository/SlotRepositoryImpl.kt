package com.stellarforge.composebooking.data.repository

import com.stellarforge.composebooking.data.model.BookedSlot
import com.stellarforge.composebooking.data.remote.SlotRemoteDataSource
import com.stellarforge.composebooking.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

class SlotRepositoryImpl @Inject constructor(
    private val slotRemoteDataSource: SlotRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SlotRepository {
    override suspend fun addSlot(slot: BookedSlot): Result<Unit> {
        return withContext(ioDispatcher) {
            slotRemoteDataSource.addSlot(slot)
        }
    }

    override suspend fun getSlotsForDate(ownerId: String, date: LocalDate): Result<List<BookedSlot>> {
        return withContext(ioDispatcher) {
            slotRemoteDataSource.getSlotsForDate(ownerId, date)
        }
    }

    override suspend fun deleteSlotByAppointmentId(appointmentId: String): Result<Unit> {
        return withContext(ioDispatcher) {
            slotRemoteDataSource.deleteSlotByAppointmentId(appointmentId)
        }
    }

}