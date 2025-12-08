package com.stellarforge.composebooking.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.firebase.Timestamp
import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.domain.repository.AppointmentRepository
import com.stellarforge.composebooking.utils.Result
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.Date

/**
 * Unit tests for [GetMyBookingsUseCase].
 *
 * Ensures that the booking history is correctly segregated into 'Upcoming' and 'Past' lists
 * based on the current timestamp. This logic is vital for the UI tab layout.
 */
class GetMyBookingsUseCaseTest {

    private val mockRepository = mockk<AppointmentRepository>()
    private val useCase = GetMyBookingsUseCase(mockRepository)

    @Test
    fun `invoke - splits bookings into upcoming and past correctly`() = runTest {
        // ARRANGE
        val now = System.currentTimeMillis()

        // Upcoming Appointment (Current time + 1 hour)
        val upcomingAppt = Appointment(
            id = "upcoming",
            appointmentDateTime = Timestamp(Date(now + 3600000))
        )
        // Past Appointment (Current time - 1 hour)
        val pastAppt = Appointment(
            id = "past",
            appointmentDateTime = Timestamp(Date(now - 3600000))
        )

        val allBookings = listOf(upcomingAppt, pastAppt)

        // Mock: Repository returns the raw list
        every { mockRepository.getMyBookingsStream("user1") } returns flowOf(Result.Success(allBookings))

        // ACT & ASSERT
        useCase("user1").test {
            val result = awaitItem()
            assertThat(result).isInstanceOf(Result.Success::class.java)

            val data = (result as Result.Success).data

            // Verify Separation Logic
            assertThat(data.upcomingBookings).containsExactly(upcomingAppt)
            assertThat(data.pastBookings).containsExactly(pastAppt)

            awaitComplete()
        }
    }
}