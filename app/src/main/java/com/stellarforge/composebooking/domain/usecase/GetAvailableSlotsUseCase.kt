package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.BookedSlot
import com.stellarforge.composebooking.data.repository.SlotRepository
import com.stellarforge.composebooking.utils.BusinessConstants
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
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
        Timber.d("GetAvailableSlotsUseCase: Invoked for date: $date, duration: $serviceDuration")
        emit(Result.Loading)
        try {
            // SlotRepository'den getSlotsForDate'in Result<List<BookedSlot>> döndürdüğünü varsayıyoruz
            // Eğer Flow<Result<List<BookedSlot>>> döndürüyorsa, .first() veya .collect {} gerekir.
            // Şimdilik suspend fun olduğunu ve direkt Result döndürdüğünü varsayalım.
            // Eğer Flow döndürüyorsa, bu kısım değişmeli.
            val slotsResult: com.stellarforge.composebooking.utils.Result<List<BookedSlot>> =
                slotRepository.getSlotsForDate(date) // Bu suspend bir fonksiyon olmalı

            when (slotsResult) {
                is com.stellarforge.composebooking.utils.Result.Success -> {
                    Timber.d("GetAvailableSlotsUseCase: Successfully fetched booked slots. Count: ${slotsResult.data.size}")
                    // Başarılıysa, randevu listesini al ve hesaplamayı yap
                    val bookedSlots: List<BookedSlot> = slotsResult.data // DÜZELTİLDİ: .data ile erişim
                    val availableTimes = calculateAvailableTimes(date, serviceDuration, bookedSlots)
                    Timber.d("GetAvailableSlotsUseCase: Calculated available times. Count: ${availableTimes.size}")
                    emit(Result.Success(availableTimes)) // Hesaplanan saatleri emit et
                }
                is com.stellarforge.composebooking.utils.Result.Error -> {
                    Timber.e(slotsResult.exception, "GetAvailableSlotsUseCase: Error fetching booked slots. Message: ${slotsResult.message}")
                    // Randevuları çekerken hata olduysa, hatayı emit et
                    emit(Result.Error(slotsResult.exception, slotsResult.message ?: "Error fetching booked slots in UseCase")) // DÜZELTİLDİ
                }
                is com.stellarforge.composebooking.utils.Result.Loading -> {
                    // Eğer slotRepository.getSlotsForDate kendisi Loading döndürebiliyorsa
                    // bu durumu tekrar emit edebiliriz. Zaten en başta emit etmiştik.
                    Timber.d("GetAvailableSlotsUseCase: slotRepository.getSlotsForDate is Loading.")
                    emit(Result.Loading)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "GetAvailableSlotsUseCase: Unexpected error during flow execution.")
            // Flow sırasında beklenmedik bir hata olursa yakala ve emit et
            emit(Result.Error(Exception("UseCase: Error calculating available slots", e)))
        }
    }.catch { e ->
        Timber.e(e, "GetAvailableSlotsUseCase: Unhandled error in flow chain.")
        emit(Result.Error(Exception("UseCase: Unhandled error calculating available slots", e)))
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
                val slotStartLocalDateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(slot.startTime.seconds, slot.startTime.nanoseconds.toLong()),
                    ZoneId.systemDefault()
                )
                val slotEndLocalDateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(slot.endTime.seconds, slot.endTime.nanoseconds.toLong()),
                    ZoneId.systemDefault()
                )

                // Sadece aynı güne ait slotları dikkate al
                if (slotStartLocalDateTime.toLocalDate() == date) {
                    val slotStart = slotStartLocalDateTime.toLocalTime()
                    val slotEnd = slotEndLocalDateTime.toLocalTime()

                    // Çakışma kontrolü: (İstenenBaşlangıç < SlotBitiş) && (İstenenBitiş > SlotBaşlangıç)
                    if (currentTime.isBefore(slotEnd) && potentialEndTime.isAfter(slotStart)) {
                        isSlotAvailable = false
                        Timber.d("Slot conflict: Request [$currentTime - $potentialEndTime] conflicts with Booked [$slotStart - $slotEnd]")
                        break
                    }
                }
            }

            if (isSlotAvailable) {
                availableSlots.add(currentTime)
            }

            currentTime = currentTime.plusMinutes(timeSlotInterval.toLong())
        }
        Timber.d("Calculated available slots for $date: $availableSlots")
        return availableSlots
    }
}