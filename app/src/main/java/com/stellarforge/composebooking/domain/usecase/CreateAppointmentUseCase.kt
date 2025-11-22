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

        if (ownerId.isBlank() || userId.isBlank() || serviceId.isBlank()) {
            val exception = IllegalArgumentException("Owner, User, or Service ID cannot be blank.")
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
            startTimestamp = Timestamp(Date.from(startLocalDateTime.atZone(zoneId).toInstant()))
            endTimestamp = Timestamp(Date.from(endLocalDateTime.atZone(zoneId).toInstant()))
        } catch (e: Exception) {
            Timber.e(e, "Error creating timestamps from Instant")
            return Result.Error(Exception("Invalid date/time format when creating Timestamps.", e))
        }

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
            createdAt = null  // @ServerTimestamp bunu dolduracak
        )

        val newSlot = BookedSlot(
            startTime = startTimestamp,
            endTime = endTimestamp
        )

        Timber.d("Attempting to create appointment and slot via a single repository call.")

        return appointmentRepository.createAppointmentAndSlot(newAppointment, newSlot)
    }
}