package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.domain.repository.AppointmentRepository
import com.stellarforge.composebooking.utils.Result
import java.time.LocalDate
import javax.inject.Inject

/**
 * UseCase responsible for retrieving the daily schedule for a specific Business Owner.
 *
 * **Purpose:**
 * This logic powers the "Owner Schedule" screen. Unlike the customer view (which only sees
 * available time slots), the owner needs to see full appointment details (Customer Name,
 * Service Type, Exact Time) for a specific date to manage their day.
 */
class GetScheduleForDateUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository
) {
    /**
     * Fetches the list of appointments for the given date.
     *
     * @param ownerId The unique UID of the business owner.
     * @param date The specific date to query (e.g., Today, Tomorrow).
     * @return [Result.Success] containing the list of appointments, or [Result.Error] if failed.
     */
    suspend operator fun invoke(ownerId: String, date: LocalDate): Result<List<Appointment>> {
        // Validation: Fail fast if the ID is missing
        if (ownerId.isBlank()) {
            return Result.Error(IllegalArgumentException("Owner ID cannot be blank."))
        }

        // Delegate to repository
        return appointmentRepository.getAppointmentsForDate(ownerId, date)
    }
}