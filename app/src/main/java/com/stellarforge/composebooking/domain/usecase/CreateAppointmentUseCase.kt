package com.stellarforge.composebooking.domain.usecase

import com.google.firebase.Timestamp
import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.data.model.BookedSlot
import com.stellarforge.composebooking.domain.repository.AppointmentRepository
import com.stellarforge.composebooking.utils.Result
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

/**
 * Orchestrates the creation of a new appointment.
 *
 * This UseCase acts as a "Gatekeeper" before writing to the database:
 * 1. **Validation:** Checks if required fields (Name, Phone) are present.
 * 2. **Data Formatting:** Converts LocalTime to Firestore Timestamp.
 * 3. **Transaction:** Calls the repository to create both [Appointment] and [BookedSlot] atomically.
 *
 * @return [Result.Success] if created, [Result.Error] if validation fails or DB error occurs.
 */
class CreateAppointmentUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository
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

        // 1. Critical System ID Validation
        if (ownerId.isBlank() || userId.isBlank() || serviceId.isBlank()) {
            val msg = "Critical IDs (Owner, User, or Service) are missing. Cannot create appointment."
            Timber.w(msg)
            return Result.Error(IllegalArgumentException(msg))
        }

        // 2. User Input Validation
        if (customerName.isBlank() || customerPhone.isBlank()) {
            val msg = "Customer name and phone number are required."
            Timber.w(msg)
            return Result.Error(IllegalArgumentException(msg))
        }

        // 3. Time Calculation & Conversion
        val startLocalDateTime = LocalDateTime.of(date, time)
        val endLocalDateTime = startLocalDateTime.plusMinutes(serviceDuration.toLong())
        val zoneId = ZoneId.systemDefault()

        val startTimestamp: Timestamp
        val endTimestamp: Timestamp
        try {
            startTimestamp = Timestamp(Date.from(startLocalDateTime.atZone(zoneId).toInstant()))
            endTimestamp = Timestamp(Date.from(endLocalDateTime.atZone(zoneId).toInstant()))
        } catch (e: Exception) {
            Timber.e(e, "Error converting local time to Firestore Timestamp.")
            return Result.Error(Exception("Invalid date/time format.", e))
        }

        // 4. Object Preparation
        val newAppointment = Appointment(
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
            createdAt = null  // @ServerTimestamp will populate this
        )

        val newSlot = BookedSlot(
            startTime = startTimestamp,
            endTime = endTimestamp
        )

        Timber.d("UseCase: Initiating atomic booking transaction...")

        // 5. Execution
        return appointmentRepository.createAppointmentAndSlot(newAppointment, newSlot)
    }
}