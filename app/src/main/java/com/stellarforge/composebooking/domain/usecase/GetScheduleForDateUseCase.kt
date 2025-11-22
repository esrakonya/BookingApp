package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.domain.repository.AppointmentRepository
import com.stellarforge.composebooking.utils.Result
import java.time.LocalDate
import javax.inject.Inject

/**
 * Belirli bir işletme sahibine ait ve belirli bir tarihteki randevuları getiren UseCase.
 */
class GetScheduleForDateUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository
) {
    /**
     * @param ownerId Randevuları listelenecek işletme sahibinin UID'si.
     * @param date Randevuların alınacağı tarih.
     * @return Randevu listesini veya hatayı içeren bir Result.
     */
    suspend operator fun invoke(ownerId: String, date: LocalDate): Result<List<Appointment>> {
        if (ownerId.isBlank()) {
            return Result.Error(IllegalArgumentException("İşletme sahibi kimliği boş olamaz."))
        }

        return appointmentRepository.getAppointmentsForDate(ownerId, date)
    }
}