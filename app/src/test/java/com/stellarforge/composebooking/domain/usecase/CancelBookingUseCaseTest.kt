package com.stellarforge.composebooking.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.domain.repository.AppointmentRepository
import com.stellarforge.composebooking.domain.repository.SlotRepository
import com.stellarforge.composebooking.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for [CancelBookingUseCase].
 *
 * CRITICAL BUSINESS LOGIC:
 * This use case handles the transactional consistency of cancelling an appointment.
 * It must ensure that BOTH the Appointment document and the Time Slot document are deleted
 * to free up the schedule for other customers.
 */
class CancelBookingUseCaseTest {

    private val mockApptRepo = mockk<AppointmentRepository>()
    private val mockSlotRepo = mockk<SlotRepository>()
    private val useCase = CancelBookingUseCase(mockApptRepo, mockSlotRepo)

    @Test
    fun `invoke - when slot deletion succeeds - deletes appointment too (Happy Path)`() = runTest {
        // ARRANGE
        coEvery { mockSlotRepo.deleteSlotByAppointmentId("appt1") } returns Result.Success(Unit)
        coEvery { mockApptRepo.deleteAppointment("appt1") } returns Result.Success(Unit)

        // ACT
        val result = useCase("appt1")

        // ASSERT
        assertThat(result).isInstanceOf(Result.Success::class.java)

        // Verification: Both operations must be called to ensure data consistency.
        coVerify(exactly = 1) { mockSlotRepo.deleteSlotByAppointmentId("appt1") }
        coVerify(exactly = 1) { mockApptRepo.deleteAppointment("appt1") }
    }

    @Test
    fun `invoke - when slot deletion fails - returns Error and DOES NOT delete appointment`() = runTest {
        // ARRANGE
        // Simulate a failure in the first step (Slot Deletion)
        coEvery { mockSlotRepo.deleteSlotByAppointmentId("appt1") } returns Result.Error(Exception("Slot fail"))

        // ACT
        val result = useCase("appt1")

        // ASSERT
        assertThat(result).isInstanceOf(Result.Error::class.java)

        // SAFETY CHECK:
        // If the slot cannot be deleted, we should not delete the appointment to prevent data corruption ("Ghost Appointments").
        coVerify(exactly = 0) { mockApptRepo.deleteAppointment(any()) }
    }
}