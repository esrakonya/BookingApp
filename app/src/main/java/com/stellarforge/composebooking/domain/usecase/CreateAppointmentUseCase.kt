package com.stellarforge.composebooking.domain.usecase

import com.google.firebase.Timestamp
import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.data.model.BookedSlot
import com.stellarforge.composebooking.data.repository.AppointmentRepository
import com.stellarforge.composebooking.data.repository.SlotRepository
import com.stellarforge.composebooking.utils.onError
import com.stellarforge.composebooking.utils.Result
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

class CreateAppointmentUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val slotRepository: SlotRepository
) {
    suspend operator fun invoke(
        ownerId: String,
        servicePriceInCents: Long,
        userId: String,
        serviceId: String,
        serviceName: String,
        serviceDuration: Int,
        date: LocalDate,
        time: LocalTime,
        customerName: String,
        customerPhone: String,
        customerEmail: String?
    ): Result<Unit> {

        if (ownerId.isBlank()) {
            val exception = IllegalArgumentException("Owner ID cannot be blank.")
            Timber.w(exception)
            return Result.Error(exception)
        }

        if (userId.isBlank()) {
            val exception = IllegalArgumentException("User ID cannot be blank.")
            Timber.w(exception)
            return Result.Error(exception)
        }
        if (serviceId.isBlank()) {
            val exception = IllegalArgumentException("Service ID cannot be blank.")
            Timber.w(exception)
            return Result.Error(exception)
        }
        if (customerName.isBlank() || customerPhone.isBlank()) {
            val exception = IllegalArgumentException("Customer name and phone cannot be empty.")
            Timber.w(exception)
            return Result.Error(exception)
        }

        val startLocalDateTime = LocalDateTime.of(date, time)
        val endLocalDateTime = startLocalDateTime.plusMinutes(serviceDuration.toLong())
        val zoneId = ZoneId.systemDefault()

        val startTimestamp: Timestamp
        val endTimestamp: Timestamp
        try {
            val startInstant = startLocalDateTime.atZone(zoneId).toInstant()
            startTimestamp = Timestamp(startInstant.epochSecond, startInstant.nano)

            val endInstant = endLocalDateTime.atZone(zoneId).toInstant()
            endTimestamp = Timestamp(endInstant.epochSecond, endInstant.nano)
        } catch (e: Exception) {
            Timber.e(e, "Error creating timestamps from Instant")
            return Result.Error(Exception("Invalid date/time format when creating Timestamps.", e))
        }

        val newAppointmentRef = try {
            appointmentRepository.getNewAppointmentReference()
        } catch (e: Exception) {
            Timber.e(e, "Error getting new appointment reference")
            return Result.Error(Exception("Failed to get appointment reference", e))
        }
        val newAppointmentId = newAppointmentRef.id

        val newAppointment = Appointment(
            id = newAppointmentId,
            ownerId = ownerId,
            userId = userId,
            serviceId = serviceId,
            serviceName = serviceName,
            servicePriceInCents = servicePriceInCents,
            durationMinutes = serviceDuration,
            appointmentDateTime = startTimestamp,
            customerName = customerName.trim(),
            customerPhone = customerPhone.trim(),
            customerEmail = customerEmail?.trim()?.takeIf { it.isNotEmpty() },
            createdAt = null  // @ServerTimestamp bunu dolduracak
        )

        val newSlot = BookedSlot(
            startTime = startTimestamp,
            endTime = endTimestamp,
            appointmentId = newAppointmentId,
            ownerId = ownerId
        )

        try {
            // 1. Önce Appointment'ı yaz
            Timber.d("Attempting to write appointment $newAppointmentId")
            val appointmentResult = appointmentRepository.createAppointmentWithId(newAppointmentRef, newAppointment)

            if (appointmentResult is Result.Error) {
                Timber.e(appointmentResult.exception, "Failed to write appointment $newAppointmentId. Slot write will not be attempted.")
                return Result.Error(appointmentResult.exception, appointmentResult.message ?: "Failed to create appointment record")
            }

            // 2. Appointment başarılıysa, Slot'u yaz
            Timber.d("Appointment write successful for $newAppointmentId. Attempting to write slot.")
            val slotResult = slotRepository.addSlot(newSlot)

            if (slotResult is Result.Error) {
                Timber.e(slotResult.exception, "Failed to write slot for appointment $newAppointmentId after successful appointment write.")
                // --- İDEALDE GERİ ALMA (ROLLBACK) ---
                // Bu noktada, başarılı bir şekilde yazılmış olan newAppointment'ı silmek gerekir.
                // Bu, bir Firestore transaction içinde yapılabilir veya "best-effort" silme denenebilir.
                // Şimdilik, sadece slot yazma hatasını döndürüyoruz.
                // Kullanıcıya "Randevunuz oluşturuldu ama takvimde görünmeyebilir, lütfen tekrar deneyin" gibi bir mesaj gösterilebilir.
                // Veya daha iyisi, bu durumu bir transaction ile yönetmek.
                // Örnek bir geri alma denemesi (deleteAppointment metodu Repository'de olmalı):
                // Timber.w("Attempting to delete successfully created appointment $newAppointmentId due to slot write failure.")
                // val deleteResult = appointmentRepository.deleteAppointment(newAppointmentId)
                // if (deleteResult.isFailure) {
                //    Timber.e(deleteResult.exceptionOrNull(), "Critical error: Failed to delete appointment $newAppointmentId after slot write failure. Data inconsistency.")
                // }
                return Result.Error(slotResult.exception ?: Exception("Failed to write slot. Appointment was created but slot booking failed."))
            }

            // Her ikisi de başarılı
            Timber.d("Successfully wrote appointment and slot for $newAppointmentId")
            return Result.Success(Unit)

        } catch (e: CancellationException) {
            Timber.w(e, "Create appointment/slot was cancelled for $newAppointmentId")
            throw e // Cancellation'ı tekrar fırlat, coroutine scope'u doğru yönetilsin
        } catch (e: Exception) {
            // Bu catch bloğu daha çok getNewAppointmentReference veya timestamp oluşturma gibi
            // beklenmedik diğer hataları yakalar. Repository çağrılarından dönen
            // Result.failure'lar yukarıda zaten ele alınıyor.
            Timber.e(e, "Unexpected error during create appointment process after initial validations for potential ID $newAppointmentId")
            return Result.Error(Exception("An unexpected error occurred during booking.", e))
        }
    }
}