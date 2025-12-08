package com.stellarforge.composebooking.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.stellarforge.composebooking.data.model.Appointment
import com.stellarforge.composebooking.domain.repository.AppointmentRepository
import com.stellarforge.composebooking.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [GetScheduleForDateUseCase].
 * Used by the Business Owner to view daily appointments.
 */
class GetScheduleForDateUseCaseTest {
    private val mockRepo = mockk<AppointmentRepository>()
    private val useCase = GetScheduleForDateUseCase(mockRepo)

    @Test
    fun `invoke - calls repository with correct params`() = runTest {
        // ARRANGE
        val date = LocalDate.now()
        val list = listOf(Appointment(id = "1"))
        coEvery { mockRepo.getAppointmentsForDate("owner1", date) } returns Result.Success(list)

        // ACT
        val result = useCase("owner1", date)

        // ASSERT
        assertThat((result as Result.Success).data).isEqualTo(list)
        coVerify { mockRepo.getAppointmentsForDate("owner1", date) }
    }

    @Test
    fun `invoke - with blank ownerId - returns Error (Validation)`() = runTest {
        // ACT
        val result = useCase("", LocalDate.now())

        // ASSERT
        assertThat(result).isInstanceOf(Result.Error::class.java)
        // Validation ensures repository is not called with invalid data
        coVerify(exactly = 0) { mockRepo.getAppointmentsForDate(any(), any()) }
    }
}