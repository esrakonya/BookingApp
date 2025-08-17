package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.BookedSlot
import com.stellarforge.composebooking.data.repository.SlotRepository
import com.stellarforge.composebooking.utils.BusinessConstants
import com.stellarforge.composebooking.utils.FirebaseConstants
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

        // Müşteri tarafı olduğu için, her zaman sabit işletme ID'sini kullanıyoruz.
        val targetOwnerId = FirebaseConstants.TARGET_BUSINESS_OWNER_ID

        try {
            // 2. Repository'den o güne ait dolu slotları al
            when (val slotsResult = slotRepository.getSlotsForDate(targetOwnerId, date)) {
                is Result.Success -> {
                    val bookedSlots = slotsResult.data
                    Timber.d("UseCase: Fetched ${bookedSlots.size} booked slots for date: $date")

                    // 3. Müsait saatleri hesapla
                    val availableSlots = calculateAvailableSlots(bookedSlots, serviceDuration)

                    // 4. Hesaplanan müsait saatleri Success olarak yay
                    emit(Result.Success(availableSlots))
                }
                is Result.Error -> {
                    Timber.e(slotsResult.exception, "UseCase: Error fetching slots from repository.")
                    // Repository'den gelen hatayı doğrudan yukarı yay
                    emit(slotsResult)
                }
                is Result.Loading -> {
                    // Repository'nin kendisi de Loading döndürebilir, bu durumu yukarı yay
                    emit(Result.Loading)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "UseCase: Error calculating available slots")
            // Hesaplama sırasında veya repository çağrısında beklenmedik bir hata olursa
            val wrappedException = Exception("UseCase: Error calculating available slots for $date", e)
            emit(Result.Error(wrappedException))
        }
    }.catch { e ->
        Timber.e(e, "GetAvailableSlotsUseCase: Unhandled error in flow chain.")
        emit(Result.Error(Exception("UseCase: Unhandled error calculating available slots", e)))
    }.flowOn(Dispatchers.Default) // Hesaplama işlemleri için Default dispatcher

    private fun calculateAvailableSlots(
        bookedSlots: List<BookedSlot>,
        serviceDurationMinutes: Int
    ): List<LocalTime> {
        val availableTimeSlots = mutableListOf<LocalTime>()
        val openingTime = BusinessConstants.OPENING_TIME
        val closingTime = BusinessConstants.CLOSING_TIME
        val interval = BusinessConstants.SLOT_INTERVAL_MINUTES.toLong()

        // Dolu saat aralıklarını LocalTime cinsinden bir listeye çevirelim (daha kolay karşılaştırma için)
        val bookedIntervals = bookedSlots.map {
            val start = it.startTime.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalTime()
            val end = it.endTime.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalTime()
            start to end
        }.sortedBy { it.first } // Başlangıç saatine göre sırala

        var potentialStartTime = openingTime
        while (potentialStartTime.plusMinutes(serviceDurationMinutes.toLong()) <= closingTime) {
            val potentialEndTime = potentialStartTime.plusMinutes(serviceDurationMinutes.toLong())

            var isSlotAvailable = true

            // Potansiyel randevu aralığının (potentialStartTime, potentialEndTime)
            // herhangi bir dolu aralıkla (bookedStart, bookedEnd) çakışıp çakışmadığını kontrol et
            for ((bookedStart, bookedEnd) in bookedIntervals) {
                // Çakışma koşulu:
                // Bir aralığın başlangıcı, diğer aralık bitmeden önceyse VE
                // o aralığın bitişi, diğer aralığın başlangıcından sonraysa çakışma vardır.
                // (StartA < EndB) and (EndA > StartB)
                if (potentialStartTime < bookedEnd && potentialEndTime > bookedStart) {
                    isSlotAvailable = false
                    break // Çakışma bulundu, daha fazla kontrol etmeye gerek yok
                }
            }

            if (isSlotAvailable) {
                availableTimeSlots.add(potentialStartTime)
            }

            // Bir sonraki potansiyel zamanı sabit aralıkla artır
            potentialStartTime = potentialStartTime.plusMinutes(interval)
        }
        return availableTimeSlots
    }
}