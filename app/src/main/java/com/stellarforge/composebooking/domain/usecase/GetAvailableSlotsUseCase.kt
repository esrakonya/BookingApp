package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.data.model.BookedSlot
import com.stellarforge.composebooking.data.model.Service
import com.stellarforge.composebooking.data.repository.AppointmentRepository
import com.stellarforge.composebooking.data.repository.SlotRepository
import com.stellarforge.composebooking.utils.BusinessConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * Belirli bir tarih ve servis süresi için uygun randevu saatlerini hesaplayan Use Case.
 */
class GetAvailableSlotsUseCase @Inject constructor(
    private val slotRepository: SlotRepository // Repository'yi enjekte et
) {
    // Use case'i fonksiyon gibi çağırmak için 'operator fun invoke' kullanabiliriz
    operator fun invoke(date: LocalDate, serviceDuration: Int): Flow<Result<List<LocalTime>>> = flow {
        // Önce o günkü randevuları repository'den çekelim
        val slotsResult = slotRepository.getSlotsForDate(date)

        if (slotsResult.isSuccess) {
            // Başarılıysa, randevu listesini al ve hesaplamayı yap
            val bookedSlots = slotsResult.getOrThrow()
            val availableTimes = calculateAvailableTimes(date, serviceDuration, bookedSlots)
            emit(Result.success(availableTimes)) // Hesaplanan saatleri emit et
        } else {
            // Randevuları çekerken hata olduysa, hatayı emit et
            emit(Result.failure(slotsResult.exceptionOrNull() ?: Exception("Unknown error fetching appointments in UseCase")))
        }
    }.catch { e ->
        // Flow sırasında beklenmedik bir hata olursa yakala ve emit et
        emit(Result.failure(Exception("UseCase: Error calculating available slots", e)))
    }.flowOn(Dispatchers.Default) // Hesaplama işlemleri için Default dispatcher

    // Müsait saatleri hesaplayan yardımcı fonksiyon (Repository'den buraya taşındı)
    private fun calculateAvailableTimes(
        date: LocalDate,
        serviceDuration: Int,
        bookedSlots: List<BookedSlot>
    ): List<LocalTime> {
        val availableSlots = mutableListOf<LocalTime>()
        val openingTime = BusinessConstants.OPENING_TIME
        val closingTime = BusinessConstants.CLOSING_TIME
        val timeSlotInterval = BusinessConstants.SLOT_INTERVAL_MINUTES

        var currentTime = openingTime
        while (currentTime.plusMinutes(serviceDuration.toLong()) <= closingTime) {
            val potentialEndTime = currentTime.plusMinutes(serviceDuration.toLong())
            var isSlotAvailable = true

            for (slot in bookedSlots) {
                // Firestore Timestamp'i LocalTime'a çevir
                val slotStart = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(slot.startTime.seconds, slot.startTime.nanoseconds.toLong()),
                    ZoneId.systemDefault() // Cihazın zaman dilimini kullanıyoruz
                ).toLocalTime()
                val slotEnd = LocalDateTime.ofInstant(Instant.ofEpochSecond(slot.endTime.seconds, slot.endTime.nanoseconds.toLong()), ZoneId.systemDefault()).toLocalTime()

                // Çakışma kontrolü: (Başlangıç < RandevuBitiş) && (Bitiş > RandevuBaşlangıç)
                if (currentTime.isBefore(slotEnd) && potentialEndTime.isAfter(slotStart)) {
                    isSlotAvailable = false
                    break
                }
            }

            if (isSlotAvailable) {
                availableSlots.add(currentTime)
            }

            currentTime = currentTime.plusMinutes(timeSlotInterval.toLong())
        }
        return availableSlots
    }
}