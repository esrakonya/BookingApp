package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.domain.repository.AppointmentRepository
import com.stellarforge.composebooking.domain.repository.SlotRepository
import com.stellarforge.composebooking.utils.Result
import timber.log.Timber
import javax.inject.Inject

/**
 * Bir randevuyu ve ona karşılık gelen dolu zaman aralığını (booked slot) iptal eden UseCase.
 *
 * Bu UseCase, veri tutarlılığını sağlamak için hem 'appointments' hem de 'bookedSlots'
 * koleksiyonlarında silme işlemi yapar.
 */
class CancelBookingUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val slotRepository: SlotRepository
) {
    /**
     * @param appointmentId İptal edilecek randevunun döküman ID'si.
     * @return İşlemin başarısını veya başarısızlığını belirten bir Result.
     */
    suspend operator fun invoke(appointmentId: String): Result<Unit> {
        if (appointmentId.isBlank()) {
            return Result.Error(IllegalArgumentException("Randevu ID'si boş olamaz."))
        }
        Timber.d("Attempting to cancel booking for appointment ID: $appointmentId")

        val slotDeleteResult = slotRepository.deleteSlotByAppointmentId(appointmentId)

        if (slotDeleteResult is Result.Error) {
            Timber.e(slotDeleteResult.exception, "Failed to delete booked slot for appointment ID: $appointmentId. Halting cancellation.")

            return Result.Error(
                Exception("Randevuya ait zaman aralığı silinemedi.", slotDeleteResult.exception),
                "Randevu iptal edilemedi. Lütfen tekrar deneyin."
            )
        }

        Timber.d("Successfully deleted booked slot for appointment ID: $appointmentId. Now deleting appointment record.")

        val appointmentDeleteResult = appointmentRepository.deleteAppointment(appointmentId)

        if (appointmentDeleteResult is Result.Error) {
            Timber.e(appointmentDeleteResult.exception, "CRITICAL: Failed to delete appointment record for ID: $appointmentId after its slot was deleted. Data inconsistency possible.")

            return Result.Error(
                Exception("Randevu kaydı silinemedi.", appointmentDeleteResult.exception),
                "Randevu iptal edilirken bir sorun oluştu."
            )
        }

        Timber.i("Successfully cancelled booking for appointment ID: $appointmentId")
        return Result.Success(Unit)
    }
}