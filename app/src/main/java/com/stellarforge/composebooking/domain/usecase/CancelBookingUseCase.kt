package com.stellarforge.composebooking.domain.usecase

import com.stellarforge.composebooking.domain.repository.AppointmentRepository
import com.stellarforge.composebooking.domain.repository.SlotRepository
import com.stellarforge.composebooking.utils.Result
import timber.log.Timber
import javax.inject.Inject

/**
 * Orchestrates the cancellation of a booking.
 *
 * **Data Consistency Strategy:**
 * This UseCase performs a sequential deletion process to maintain calendar integrity:
 * 1. **Delete BookedSlot:** First, it attempts to remove the occupied time slot. This immediately frees up the calendar for new bookings.
 * 2. **Delete Appointment:** If the slot deletion is successful, it removes the actual appointment details.
 *
 * This order minimizes the risk of "Double Booking" (a slot remaining occupied for a deleted appointment).
 */
class CancelBookingUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val slotRepository: SlotRepository
) {
    /**
     * Executes the cancellation logic.
     *
     * @param appointmentId The unique document ID of the appointment to cancel.
     * @return [Result.Success] if both operations succeed, or [Result.Error] otherwise.
     */
    suspend operator fun invoke(appointmentId: String): Result<Unit> {
        // 1. Validation
        if (appointmentId.isBlank()) {
            return Result.Error(IllegalArgumentException("Appointment ID cannot be blank."))
        }
        Timber.d("Attempting to cancel booking for appointment ID: $appointmentId")

        // 2. Step 1: Free up the time slot
        val slotDeleteResult = slotRepository.deleteSlotByAppointmentId(appointmentId)

        if (slotDeleteResult is Result.Error) {
            Timber.e(slotDeleteResult.exception, "Failed to delete booked slot for appointment ID: $appointmentId. Halting cancellation.")

            return Result.Error(
                Exception("Failed to delete the associated time slot.", slotDeleteResult.exception),
                "Could not cancel booking. Please try again."
            )
        }

        Timber.d("Successfully deleted booked slot for appointment ID: $appointmentId. Now deleting appointment record.")

        // 3. Step 2: Remove the appointment record
        val appointmentDeleteResult = appointmentRepository.deleteAppointment(appointmentId)

        if (appointmentDeleteResult is Result.Error) {
            // Critical Edge Case: Slot is gone, but Appointment remains. This is a partial failure.
            // In a production app, we might want to flag this for manual review or retry.
            Timber.e(appointmentDeleteResult.exception, "CRITICAL: Failed to delete appointment record for ID: $appointmentId after its slot was deleted. Data inconsistency possible.")

            return Result.Error(
                Exception("Failed to delete appointment record.", appointmentDeleteResult.exception),
                "An error occurred while canceling the appointment."
            )
        }

        Timber.i("Successfully cancelled booking for appointment ID: $appointmentId")
        return Result.Success(Unit)
    }
}